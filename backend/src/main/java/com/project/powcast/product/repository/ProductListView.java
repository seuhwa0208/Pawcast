package com.project.powcast.product.repository;

import java.math.BigDecimal;

// Product.java, ProductVariant.java, FoodDetail.java, DeviceDetail.java, ProductImage.java의 내용 중
// 리스트에 표시할 수 있는 내용을 하나의 java 파일에 모은 것
public interface ProductListView {

    Long getId();

    String getName();

    String getBrand();

    String getCategoryName();

    String getImagePath();

    Long getVariantId();

    BigDecimal getOriginalPrice();

    BigDecimal getSalePrice();

    BigDecimal getDiscountRate();

    Long getSalesCount();

    Long getWishCount();

    Long getReviewCount();

    BigDecimal getAverageRating();

    Boolean getSoldOut();
}
