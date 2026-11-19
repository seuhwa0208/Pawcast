package com.project.pawcast.product.service;

import com.project.pawcast.product.entity.Category;
import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.product.dto.*;
import com.project.pawcast.product.entity.*;
import com.project.pawcast.product.repository.*;
import com.project.pawcast.review.dto.BestReviewResponse;
import com.project.pawcast.review.service.ReviewService;
import com.project.pawcast.security.AuthorizationValidator;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {
    private final ProductRepository productRepository;
    private final ProductVariantRepository variantRepository;
    private final ProductImageRepository imageRepository;
    private final FoodDetailRepository foodRepository;
    private final DeviceDetailRepository deviceRepository;
    private final CategoryService categoryService;
    private final ReviewService reviewService;
    private final MembersRepository membersRepository;
    private final AuthorizationValidator authorizationValidator;
    private final Validator validator;

    // 관리자 검색
    public Product findById(Long id) {
        return productRepository.findManagementProductById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PRODUCT));
    }

    public List<ProductResponse> searchProducts(ProductSearchRequest request) {
        validateRequest(request);
        if (request.minPrice() != null && request.maxPrice() != null
                && request.minPrice().compareTo(request.maxPrice()) > 0) {
            throw new BusinessException(ErrorCode.INVALID_PRICE_RANGE);
        }

        List<ProductListView> products = productRepository.searchProducts(
                request.keyword(), request.productType(), request.categoryId(), request.brand(),
                request.ingredient(), request.foodKind(), request.foodForm(), request.lifeStage(),
                request.minPrice(), request.maxPrice(), request.minRating(), request.inStockOnly(),
                request.sortType()
        );
        Map<Long, BestReviewResponse> bestReviews = reviewService.findBestReviews(
                products.stream().map(ProductListView::getId).toList()
        );
        return products.stream()
                .map(product -> ProductResponse.from(product, bestReviews.get(product.getId())))
                .toList();
    }

    public List<ProductResponse> bestReviewProducts(ProductSearchRequest request) {
        return searchProducts(request).stream()
                .filter(product -> product.bestReview() != null)
                .sorted(Comparator.comparing(ProductResponse::averageRating).reversed()
                        .thenComparing(ProductResponse::id))
                .limit(5)
                .toList();
    }

    public List<String> brands() {
        return productRepository.findBrands();
    }

    public ProductDetailResponse detail(Long productId) {
        Product product = productRepository.findPublicProductById(productId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PRODUCT));
        return response(product, false);
    }


    public List<ProductResponse> managementList(Long memberId) {
        validateManager(memberId);
        return productRepository.findAllByOrderByIdAsc().stream()
                .map(this::managementResponse)
                .toList();
    }

    public ProductDetailResponse managementDetail(Long memberId, Long productId) {
        validateManager(memberId);
        return response(findById(productId), true);
    }

    @Transactional
    public ProductDetailResponse create(Long memberId, ProductCreateRequest request) {
        validateManager(memberId);
        validateCreateRequest(request);
        Category category = categoryService.findById(request.categoryId());
        Product product = productRepository.save(Product.builder()
                .name(request.name().strip())
                .brand(request.brand())
                .productType(request.productType())
                .category(category)
                .description(request.description())
                .build());

        for (VariantCreateRequest variant : request.variants()) {
            addVariant(product, variant);
        }
        replaceImages(product, request.images());
        saveDetails(product, request.foodDetail(), request.deviceDetail());
        return response(product, true);
    }

    @Transactional
    public ProductDetailResponse update(Long memberId, Long productId, ProductUpdateRequest request) {
        validateManager(memberId);
        validateRequest(request);
        validateImages(request.images());
        Product product = findById(productId);
        validateDetailType(product.getProductType(), request.foodDetail(), request.deviceDetail(), false);

        if (request.name() != null) {
            if (request.name().isBlank()) {
                throw new BusinessException(ErrorCode.INVALID_PRODUCT_REQUEST);
            }
            product.setName(request.name().strip());
        }
        if (request.brand() != null) {
            product.setBrand(request.brand());
        }
        if (request.categoryId() != null) {
            product.setCategory(categoryService.findById(request.categoryId()));
        }
        if (request.description() != null) {
            product.setDescription(request.description());
        }
        if (request.images() != null) {
            replaceImages(product, request.images());
        }
        saveDetails(product, request.foodDetail(), request.deviceDetail());
        return response(product, true);
    }

    @Transactional
    public ProductDetailResponse createVariant(Long memberId, Long productId, VariantCreateRequest request) {
        validateManager(memberId);
        Product product = findById(productId);
        addVariant(product, request);
        return response(product, true);
    }

    @Transactional
    public ProductDetailResponse updateVariant(
            Long memberId, Long productId, Long variantId, VariantUpdateRequest request
    ) {
        validateManager(memberId);
        validateRequest(request);
        ProductVariant variant = lockedVariant(variantId);
        if (!variant.getProduct().getId().equals(productId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND_PRODUCT_VARIANT);
        }
        validateVariantFields(variant.getProduct().getProductType(), request.color(), request.weightG(),
                request.volumeMl(), request.capacityMl(), request.heightCm());
        if (variantRepository.existsBySkuCodeAndIdNot(request.skuCode(), variantId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_SKU);
        }
        if (request.status() == ProductVariantStatus.ON_SALE && variant.getStock() == 0) {
            throw new BusinessException(ErrorCode.INSUFFICIENT_STOCK);
        }

        variant.setSkuCode(request.skuCode());
        variant.setOptionKey(request.optionKey());
        variant.setOptionLabel(request.optionLabel());
        variant.setColor(request.color());
        variant.setWeightG(request.weightG());
        variant.setVolumeMl(request.volumeMl());
        variant.setCapacityMl(request.capacityMl());
        variant.setHeightCm(request.heightCm());
        variant.setPrice(request.price());
        variant.setCostPrice(request.costPrice());
        variant.setProductVariantStatus(request.status());
        try {
            variantRepository.flush();
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.PRODUCT_DATA_CONFLICT);
        }
        return response(variant.getProduct(), true);
    }

    // 판매 종료
    @Transactional
    public void discontinue(Long memberId, Long productId) {
        // 관리자 권한 확인
        validateManager(memberId);

        // 상품 존재 여부 확인
        findById(productId);

        // 처음부터 옵션을 잠금 조회
        List<ProductVariant> variants =
                variantRepository.findAllByProductIdForUpdate(productId);

        // 재고·가격은 유지하고 판매 상태만 변경
        for (ProductVariant variant : variants) {
            variant.setProductVariantStatus(
                    ProductVariantStatus.DISCONTINUED
            );
        }
    }

    @Transactional
    public ProductDetailResponse adjustStock(Long memberId, Long variantId, int quantityChange) {
        validateManager(memberId);
        ProductVariant variant = lockedVariant(variantId);
        Long productId = variant.getProduct().getId();
        long stockAfter = (long) variant.getStock() + quantityChange;
        if (quantityChange == 0 || stockAfter < 0 || stockAfter > Integer.MAX_VALUE) {
            throw new BusinessException(ErrorCode.INVALID_STOCK_ADJUSTMENT);
        }
        ProductVariantStatus status = variant.getProductVariantStatus();
        if (status != ProductVariantStatus.DISCONTINUED) {
            status = stockAfter == 0 ? ProductVariantStatus.SOLD_OUT : ProductVariantStatus.ON_SALE;
        }
        if (variantRepository.updateStock(variantId, (int) stockAfter, status) != 1) {
            throw new BusinessException(ErrorCode.NOT_FOUND_PRODUCT_VARIANT);
        }
        return response(findById(productId), true);
    }

    private ProductVariant lockedVariant(Long variantId) {
        return variantRepository.findByIdForUpdate(variantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PRODUCT_VARIANT));
    }

    private void validateManager(Long memberId) {
        if (memberId == null) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        Members member = membersRepository.findById(memberId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        if (member.getStatus() != Members.Status.ACTIVE) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        authorizationValidator.validateManager(member);
    }

    private void validateRequest(Object request) {
        if (request == null || !validator.validate(request).isEmpty()) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_REQUEST);
        }
    }

    private void validateCreateRequest(ProductCreateRequest request) {
        validateRequest(request);
        validateDetailType(request.productType(), request.foodDetail(), request.deviceDetail(), true);
        validateImages(request.images());
        Set<String> skuCodes = new HashSet<>();
        for (VariantCreateRequest variant : request.variants()) {
            if (!skuCodes.add(variant.skuCode())) {
                throw new BusinessException(ErrorCode.DUPLICATE_SKU);
            }
            validateVariantFields(request.productType(), variant.color(), variant.weightG(),
                    variant.volumeMl(), variant.capacityMl(), variant.heightCm());
        }
    }

    // 상품 타입 검증
    private void validateDetailType(
            ProductType type, FoodDetailCreateRequest food, DeviceDetailCreateRequest device, boolean required
    ) {
        boolean wrongType = switch (type) {
            case DOG, CAT -> device != null;
            case DEVICE -> food != null;
        };

        boolean missingDetail = required && switch (type) {
            case DOG, CAT -> food == null;
            case DEVICE -> device == null;
        };
        if (wrongType || missingDetail) {
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_DETAIL);
        }
    }

    private void validateImages(List<ImageCreateRequest> images) {
        if (images != null && images.stream().filter(ImageCreateRequest::main).count() > 1) {
            throw new BusinessException(ErrorCode.MULTIPLE_MAIN_IMAGES);
        }
    }

    private void validateVariantFields(
            ProductType type, String color, Integer weight, Integer volume, Integer capacity, Integer height
    ) {
        boolean invalid = switch (type) {
            case DOG, CAT -> (color != null && !color.isBlank()) || capacity != null || height != null;
            case DEVICE -> weight != null || volume != null;
        };
        if (invalid) {
            throw new BusinessException(ErrorCode.INVALID_VARIANT_FIELDS);
        }
    }

    private ProductVariant addVariant(Product product, VariantCreateRequest request) {
        validateRequest(request);
        validateVariantFields(product.getProductType(), request.color(), request.weightG(),
                request.volumeMl(), request.capacityMl(), request.heightCm());
        if (variantRepository.existsBySkuCode(request.skuCode())) {
            throw new BusinessException(ErrorCode.DUPLICATE_SKU);
        }
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .skuCode(request.skuCode())
                .optionKey(request.optionKey())
                .optionLabel(request.optionLabel())
                .color(request.color())
                .weightG(request.weightG())
                .volumeMl(request.volumeMl())
                .capacityMl(request.capacityMl())
                .heightCm(request.heightCm())
                .price(request.price())
                .costPrice(request.costPrice())
                .stock(request.stock())
                .productVariantStatus(request.stock() == 0
                        ? ProductVariantStatus.SOLD_OUT : ProductVariantStatus.ON_SALE)
                .build();
        try {
            return variantRepository.saveAndFlush(variant);
        } catch (DataIntegrityViolationException exception) {
            throw new BusinessException(ErrorCode.PRODUCT_DATA_CONFLICT);
        }
    }

    private void replaceImages(Product product, List<ImageCreateRequest> images) {
        imageRepository.deleteAllByProductId(product.getId());
        imageRepository.flush();
        imageRepository.saveAll(images.stream()
                .map(image -> ProductImage.builder()
                        .product(product)
                        .imagePath(image.imagePath())
                        .main(image.main())
                        .sortOrder(image.sortOrder())
                        .build())
                .toList());
    }

    private void saveDetails(Product product, FoodDetailCreateRequest food, DeviceDetailCreateRequest device) {
        if (food != null) {
            foodRepository.saveAndFlush(FoodDetail.builder()
                    .productId(product.getId()).product(product)
                    .foodKind(food.foodKind()).foodForm(food.foodForm())
                    .mainIngredient(food.mainIngredient()).lifeStage(food.lifeStage())
                    .ingredientDescription(food.ingredientDescription()).build());
        }
        if (device != null) {
            deviceRepository.saveAndFlush(DeviceDetail.builder()
                    .productId(product.getId()).product(product)
                    .deviceKind(device.deviceKind()).material(device.material())
                    .featureDescription(device.featureDescription()).build());
        }
    }

    private ProductDetailResponse response(Product product, boolean management) {
        List<ProductVariant> variants = variantRepository.findAllByProductIdOrderByIdAsc(product.getId())
                .stream()
                .filter(variant -> management || variant.getProductVariantStatus() != ProductVariantStatus.DISCONTINUED)
                .toList();
        return ProductDetailResponse.from(product, variants,
                imageRepository.findAllByProductIdOrderBySortOrderAscIdAsc(product.getId()),
                foodRepository.findById(product.getId()).orElse(null),
                deviceRepository.findById(product.getId()).orElse(null));
    }

    private ProductResponse managementResponse(Product product) {
        List<ProductVariant> variants = variantRepository.findAllByProductIdOrderByIdAsc(product.getId());
        ProductVariant variant = variants.stream()
                .min(Comparator.comparing(ProductVariant::getPrice).thenComparing(ProductVariant::getId))
                .orElse(null);
        List<ProductImage> images = imageRepository.findAllByProductIdOrderBySortOrderAscIdAsc(product.getId());
        ProductImage mainImage = images.stream().filter(ProductImage::isMain).findFirst()
                .orElse(images.isEmpty() ? null : images.getFirst());
        BigDecimal average = product.getReviewCount() == 0
                ? BigDecimal.ZERO.setScale(1)
                : BigDecimal.valueOf(product.getRatingSum())
                        .divide(BigDecimal.valueOf(product.getReviewCount()), 1, RoundingMode.HALF_UP);
        boolean soldOut = variants.stream().noneMatch(option ->
                option.getProductVariantStatus() == ProductVariantStatus.ON_SALE && option.getStock() > 0);
        return new ProductResponse(
                product.getId(), product.getName(), product.getBrand(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                mainImage == null ? null : mainImage.getImagePath(),
                variant == null ? null : variant.getId(),
                variant == null ? null : variant.getPrice(),
                product.getSalesCount(), product.getWishCount(), product.getReviewCount(),
                average, soldOut, null
        );
    }

    // 베스트3 판매량 기준
    @Transactional(readOnly = true)
    public List<ProductResponse> getBestProducts(Long memberId) {
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        authorizationValidator.validateManager(member);
        return productRepository
                .findTop3ByDeletedAtIsNullOrderBySalesCountDescIdDesc()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }

    // 워스트 3
    @Transactional(readOnly = true)
    public List<ProductResponse> getWorstProducts(Long memberId) {
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        authorizationValidator.validateManager(member);
        return productRepository
                .findTop3ByDeletedAtIsNullOrderBySalesCountAscIdDesc()
                .stream()
                .map(ProductResponse::from)
                .toList();
    }
}
