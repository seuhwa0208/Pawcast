package com.project.pawcast.product.controller;
import com.project.pawcast.common.exception.*;
import com.project.pawcast.product.dto.*;
import com.project.pawcast.product.entity.ProductVariantStatus;
import com.project.pawcast.product.service.ProductService;
import com.project.pawcast.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/admin")
public class ProductManagementController {
    private final ProductService productService;
    private Long id(LoginMember member) {
        if(member==null) throw new BusinessException(ErrorCode.FORBIDDEN);
        return member.getId();
    }
    @GetMapping("/products")
    public List<ProductResponse> list(@AuthenticationPrincipal LoginMember member){
        return productService.managementList(id(member));
    }

    @GetMapping("/products/{productId}")
    public ProductDetailResponse detail(@AuthenticationPrincipal LoginMember member, @PathVariable Long productId) {
        return productService.managementDetail(id(member),productId);
    }

    @PostMapping(value = "/products", consumes = MediaType.MULTIPART_FORM_DATA_VALUE) @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ProductDetailResponse create(@AuthenticationPrincipal LoginMember member,
                                        @Valid @RequestPart("request") ProductCreateRequest request,
                                        @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return productService.create(id(member),request,images);
    }

    @PatchMapping( value = "/products/{productId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ProductDetailResponse update(@AuthenticationPrincipal LoginMember member,
                                        @PathVariable Long productId,
                                        @Valid @RequestPart("request") ProductUpdateRequest request,
                                        @RequestPart(value = "images", required = false) List<MultipartFile> images) {
        return productService.update(id(member),productId,request, images);
    }

    @PostMapping("/products/{productId}/variants")
    @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ProductDetailResponse addVariant(@AuthenticationPrincipal LoginMember member,
                                            @PathVariable Long productId,
                                            @Valid @RequestBody VariantCreateRequest request) {
        return productService.createVariant(id(member),productId,request);
    }

    @PatchMapping("/products/{productId}/variants/{variantId}")
    public ProductDetailResponse updateVariant(@AuthenticationPrincipal LoginMember member,
                                               @PathVariable Long productId, @PathVariable Long variantId,
                                               @Valid @RequestBody VariantUpdateRequest request) {
        return productService.updateVariant(id(member),productId,variantId,request);
    }

    @DeleteMapping("/products/{productId}")
    @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void discontinue(@AuthenticationPrincipal LoginMember member, @PathVariable Long productId) {
        productService.discontinue(id(member),productId);
    }

    @PatchMapping("/variants/{variantId}/stock")
    public ProductDetailResponse adjustStock(@AuthenticationPrincipal LoginMember member,
                                             @PathVariable Long variantId,
                                             @RequestParam int quantityChange) {
        return productService.adjustStock(id(member), variantId, quantityChange);
    }

    @PatchMapping("/variants/{variantId}/status")
    public void changeVariantStatus(@AuthenticationPrincipal LoginMember member,
                                    @PathVariable Long variantId,
                                    @RequestParam ProductVariantStatus status) {
        productService.changeVariantStatus(id(member), variantId, status);
    }

    /** 관리자 상품 목록의 상품 단위 상태 변경 결과를 갱신된 집계 상태로 반환한다. */
    @PatchMapping("/products/{productId}/status")
    public ProductResponse changeProductStatus(@AuthenticationPrincipal LoginMember member,
                                               @PathVariable Long productId,
                                               @RequestParam ProductVariantStatus status) {
        return productService.changeProductStatus(id(member), productId, status);
    }
}
