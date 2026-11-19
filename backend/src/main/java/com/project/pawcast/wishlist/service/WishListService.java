package com.project.pawcast.wishlist.service;

import com.project.pawcast.common.exception.BusinessException;
import com.project.pawcast.common.exception.ErrorCode;
import com.project.pawcast.member.entity.Members;
import com.project.pawcast.member.repository.MembersRepository;
import com.project.pawcast.product.entity.Product;
import com.project.pawcast.product.repository.ProductRepository;
import com.project.pawcast.wishlist.dto.WishListResponse;
import com.project.pawcast.wishlist.entity.WishList;
import com.project.pawcast.wishlist.repository.WishListRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class WishListService {

    private final WishListRepository wishListsRepository;
    private final MembersRepository membersRepository;
    private final ProductRepository productRepository;

    //단순 찜_ID와 멤버_ID로 배송지 조회
    public WishList findByIdAndMemberId(Long wishListId, Long memberId){
        return wishListsRepository.findByIdAndMemberId(wishListId, memberId)
                .orElseThrow(()->new BusinessException(ErrorCode.NOT_FOUND_WISHLIST));
    }

    // 찜 목록 조회
    public List<WishListResponse> listWishList(Long memberId){
        return wishListsRepository.findAllByMember_Id(memberId).stream().map(WishListResponse::from).toList();
    }
    
    // 찜 추가
    @Transactional
    public WishListResponse createWishList(Long memberId, Long productId){
        Members member = membersRepository.findById(memberId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_MEMBER));
        Product product = productRepository.findById(productId).orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_PRODUCT));

        WishList wishList = WishList.builder()
                .member(member)
                .product(product)
                .build();
        return WishListResponse.from(wishListsRepository.save(wishList));
    }
    
    // 삭제
    @Transactional
    public void deleteWishList(Long wishListId, Long memberId){
        WishList wishList = findByIdAndMemberId(wishListId, memberId);
        wishListsRepository.delete(wishList);
    }
}
