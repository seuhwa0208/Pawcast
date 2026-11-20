package com.project.pawcast.product.controller;
import com.project.pawcast.common.exception.*;
import com.project.pawcast.product.dto.*;
import com.project.pawcast.product.service.ProductService;
import com.project.pawcast.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
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

    @PostMapping("/products") @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public ProductDetailResponse create(@AuthenticationPrincipal LoginMember member, @Valid @RequestBody ProductCreateRequest request) {
        return productService.create(id(member),request);
    }

    @PatchMapping("/products/{productId}")
    public ProductDetailResponse update(@AuthenticationPrincipal LoginMember member,
                                        @PathVariable Long productId,
                                        @Valid @RequestBody ProductUpdateRequest request) {
        return productService.update(id(member),productId,request);
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
}
