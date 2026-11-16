package com.project.powcast.product.service;
import com.project.powcast.common.exception.*;
import com.project.powcast.product.dto.*;
import com.project.powcast.product.entity.ProductType;
import jakarta.validation.Validator;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import java.util.HashSet;
@Component
@RequiredArgsConstructor
public class ProductCreateValidator {
    private final Validator validator;
    public void validate(Object request) {
        if (request==null || !validator.validate(request).isEmpty()) throw new BusinessException(ErrorCode.INVALID_PRODUCT_REQUEST);
    }
    public void validateCreate(ProductCreateRequest r) {
        validate(r);
        if (r.productType()==ProductType.FOOD ? r.foodDetail()==null || r.deviceDetail()!=null : r.deviceDetail()==null || r.foodDetail()!=null)
            throw new BusinessException(ErrorCode.INVALID_PRODUCT_DETAIL);
        validateImages(r.images());
        var skus=new HashSet<String>();
        for (var v:r.variants()) {
            if (!skus.add(v.skuCode())) throw new BusinessException(ErrorCode.DUPLICATE_SKU);
            validateVariant(r.productType(),v.color(),v.weightG(),v.volumeMl(),v.capacityMl(),v.heightCm());
        }
    }
    public void validateImages(java.util.List<ImageCreateRequest> images) {
        if (images!=null && images.stream().filter(ImageCreateRequest::main).count()>1) throw new BusinessException(ErrorCode.MULTIPLE_MAIN_IMAGES);
    }
    public void validateVariant(ProductType type, String color, Integer weight, Integer volume, Integer capacity, Integer height) {
        if (type==ProductType.FOOD ? color!=null && !color.isBlank() || capacity!=null || height!=null : weight!=null || volume!=null)
            throw new BusinessException(ErrorCode.INVALID_VARIANT_FIELDS);
    }
}
