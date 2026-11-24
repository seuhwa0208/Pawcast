package com.project.pawcast.wishlist.repository;

import com.project.pawcast.wishlist.entity.WishList;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WishListRepository extends JpaRepository<WishList, Long> {
    List<WishList> findAllByMember_Id(Long memberId);
    Optional<WishList> findByIdAndMember_Id(Long wishListId, Long memberId);
    Optional<WishList> findByMember_IdAndProduct_Id(Long memberId, Long productId);
    Optional<WishList> deleteByMember_IdAndProduct_Id(Long memberId, Long productId);
}