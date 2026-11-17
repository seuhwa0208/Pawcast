package com.project.powcast.review.service;

import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import com.project.powcast.member.entity.Members;
import com.project.powcast.member.repository.MemberRepository;
import com.project.powcast.review.event.ReviewAnalysisRequested;
import com.project.powcast.security.AuthorizationValidator;
import com.project.powcast.order.entity.OrderItem;
import com.project.powcast.order.entity.OrderStatus;
import com.project.powcast.order.repository.OrderItemRepository;
import com.project.powcast.product.repository.ProductRepository;
import com.project.powcast.review.dto.*;
import com.project.powcast.review.entity.ProductReview;
import com.project.powcast.review.entity.ReviewImage;
import com.project.powcast.review.repository.ProductReviewRepository;
import com.project.powcast.review.repository.ReviewImageRepository;
import com.project.powcast.review.repository.ReviewSentimentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ProductReviewRepository reviewRepository;
    private final ReviewImageRepository reviewImageRepository;
    private final ReviewSentimentRepository reviewSentimentRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductRepository productRepository;
    private final MemberRepository memberRepository;
    private final AuthorizationValidator authorizationValidator;
    private final ApplicationEventPublisher eventPublisher;

    // 리뷰 등록
    @Transactional
    public ReviewResponse createReview(
            Long memberId,
            ReviewCreateRequest request
    ) {
        validateLogin(memberId);

        OrderItem orderItem = orderItemRepository
                .findByIdForUpdate(request.orderItemId())
                .orElseThrow(
                        () -> new BusinessException(
                                ErrorCode.NOT_FOUND_ORDER_ITEM
                        )
                );

        // 본인 주문인지 확인
        Members orderMember = orderItem.getOrder().getMember();

        if (orderMember == null
                || !memberId.equals(orderMember.getId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        // 배송 완료된 주문만 리뷰 작성 가능
        if (orderItem.getOrder().getStatus() != OrderStatus.DELIVERED) {
            throw new BusinessException(ErrorCode.REVIEW_NOT_ALLOWED);
        }

        // 주문 항목당 리뷰 한 개
        if (reviewRepository.existsByOrderItemId(orderItem.getId())) {
            throw new BusinessException(ErrorCode.DUPLICATE_REVIEW);
        }

        ProductReview review = ProductReview.builder()
                .member(orderMember)
                .product(orderItem.getProductVariant().getProduct())
                .orderItem(orderItem)
                .rating(request.rating())
                .content(request.content())
                .isVisible(true)
                .build();

        reviewRepository.saveAndFlush(review);

        saveImages(review, request.images());
        reviewImageRepository.flush();

        ReviewResponse response = toResponse(review);

        // 통계 갱신 시 영속성 컨텍스트가 초기화되므로 마지막에 호출
        checkStatistics(
                productRepository.addReviewStatistics(
                        review.getProduct().getId(),
                        review.getRating()
                )
        );

        // 추가: 리뷰 분석 요청 이벤트 발행
        eventPublisher.publishEvent(
                new ReviewAnalysisRequested(
                        response.id(),
                        response.content()
                )
        );

        return response;
    }

    // 공개 리뷰 단건 조회
    public ReviewResponse getReview(Long reviewId) {
        ProductReview review = reviewRepository.findById(reviewId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.NOT_FOUND_REVIEW)
                );

        if (!review.isVisible()) {
            throw new BusinessException(ErrorCode.NOT_FOUND_REVIEW);
        }

        return toResponse(review);
    }

    // 상품별 공개 리뷰 목록 조회
    public ProductReviewsResponse getReviews(Long productId) {
        productRepository.findPublicProductById(productId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.NOT_FOUND_PRODUCT)
                );

        List<ReviewResponse> reviews = reviewRepository
                .findVisibleReviews(productId)
                .stream()
                .map(this::toResponse)
                .toList();

        BigDecimal averageRating = reviews.isEmpty()
                ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(
                reviews.stream()
                        .mapToLong(ReviewResponse::rating)
                        .sum()
        ).divide(
                BigDecimal.valueOf(reviews.size()),
                1,
                RoundingMode.HALF_UP
        );

        return new ProductReviewsResponse(
                reviews.size(),
                averageRating,
                reviews
        );
    }

    // 상품별 대표 리뷰 조회 - 기존 ProductService에서 사용
    public Map<Long, BestReviewResponse> findBestReviews(
            List<Long> productIds
    ) {
        if (productIds.isEmpty()) {
            return Map.of();
        }

        return reviewRepository.findBestReviews(productIds)
                .stream()
                .collect(Collectors.toMap(
                        review -> review.getProduct().getId(),
                        BestReviewResponse::from
                ));
    }

    // 리뷰 수정
    @Transactional
    public ReviewResponse updateReview(
            Long reviewId,
            Long memberId,
            ReviewUpdateRequest request
    ) {
        ProductReview review = findOwnedReview(reviewId, memberId);

        int oldRating = review.getRating();
        Long productId = review.getProduct().getId();

        // 1. 기존 본문과 비교
        boolean contentChanged =
                !Objects.equals(review.getContent(), request.content());

        // 2. 바뀌었으면 기존 리뷰 삭제
        if (contentChanged) {
            reviewSentimentRepository.deleteByReviewId(reviewId);
        }

        // 3. 리뷰 내용 수정
        review.setRating(request.rating());
        review.setContent(request.content());

        // 기존 이미지 처리 유지
        // null: 유지 / 빈 목록: 전체 삭제 / 목록: 전체 교체
        if (request.images() != null) {
            reviewImageRepository.deleteAllByReviewId(reviewId);
            reviewImageRepository.flush();

            saveImages(review, request.images());
        }

        // 변경 내용과 updatedAt 반영
        reviewRepository.saveAndFlush(review);

        ReviewResponse response = toResponse(review);

        // 기존 별점 통계 갱신 유지
        if (oldRating != request.rating()) {
            checkStatistics(
                    productRepository.updateReviewStatistics(
                            productId,
                            oldRating,
                            request.rating()
                    )
            );
        }

        // 4. 본문이 바뀐 경우만 재분석 이벤트를 발행
        if (contentChanged) {
            eventPublisher.publishEvent(
                    new ReviewAnalysisRequested(
                            response.id(),
                            response.content()
                    )
            );
        }

        return response;
    }

    // 작성자 또는 관리자만 리뷰 삭제 가능
    @Transactional
    public void deleteReview(Long reviewId, Long memberId) {
        validateLogin(memberId);

        Members member = memberRepository.findById(memberId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER)
                );

        ProductReview review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.NOT_FOUND_REVIEW)
                );

        // 작성자가 아니면 관리자 권한 확인
        if (!review.getMember().getId().equals(memberId)) {
            authorizationValidator.validateAdmin(member);
        }

        Long productId = review.getProduct().getId();
        int rating = review.getRating();

        reviewSentimentRepository.deleteByReviewId(reviewId);
        reviewImageRepository.deleteAllByReviewId(reviewId);

        reviewSentimentRepository.flush();
        reviewImageRepository.flush();

        reviewRepository.delete(review);
        reviewRepository.flush();

        checkStatistics(
                productRepository.removeReviewStatistics(productId, rating)
        );
    }

    // 수정·삭제용 리뷰 조회 및 작성자 확인
    private ProductReview findOwnedReview(
            Long reviewId,
            Long memberId
    ) {
        validateLogin(memberId);

        ProductReview review = reviewRepository.findByIdForUpdate(reviewId)
                .orElseThrow(
                        () -> new BusinessException(ErrorCode.NOT_FOUND_REVIEW)
                );

        if (!review.getMember().getId().equals(memberId)) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }

        return review;
    }

    // 이미지 저장
    private void saveImages(
            ProductReview review,
            List<ReviewImageCreateRequest> requests
    ) {
        List<ReviewImage> images = requests.stream()
                .map(request -> {
                    ReviewImage image = new ReviewImage();
                    image.setReview(review);
                    image.setImagePath(request.imagePath());
                    image.setSortOrder(request.sortOrder());
                    return image;
                })
                .toList();

        reviewImageRepository.saveAll(images);
    }

    // Entity → 응답 DTO
    private ReviewResponse toResponse(ProductReview review) {
        List<ReviewImage> images = reviewImageRepository
                .findAllByReviewIdOrderBySortOrderAscIdAsc(
                        review.getId()
                );

        var sentiment = reviewSentimentRepository
                .findByReviewId(review.getId())
                .orElse(null);

        return ReviewResponse.from(
                review,
                images,
                sentiment
        );
    }

    private void validateLogin(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.TOKEN_INVALID);
        }
    }

    // 통계 갱신 실패 시 전체 작업 롤백
    private void checkStatistics(int updated) {
        if (updated != 1) {
            throw new BusinessException(ErrorCode.PRODUCT_DATA_CONFLICT);
        }
    }
}