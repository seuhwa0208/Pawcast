package com.project.pawcast.product.controller;

import com.project.pawcast.product.dto.ProductDetailResponse;
import com.project.pawcast.product.dto.ProductResponse;
import com.project.pawcast.product.dto.ProductSearchRequest;
import com.project.pawcast.product.service.ProductService;
import com.project.pawcast.security.LoginMember;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class ProductController {
    private final ProductService productService;

    @GetMapping("/products")
    public List<ProductResponse> list(@Valid @ModelAttribute ProductSearchRequest request) {
        return productService.searchProducts(request);
    }

    @GetMapping("/products/{productId}")
    public ProductDetailResponse detail(@PathVariable Long productId) {
        return productService.detail(productId);
    }

    @GetMapping("/products/best-reviews")
    public List<ProductResponse> bestReviews(@Valid @ModelAttribute ProductSearchRequest request) {
        return productService.bestReviewProducts(request);
    }

    @GetMapping("/brands")
    public List<String> brands() {
        return productService.brands();
    }


    // 판매량 BEST 3 - > 관리자 매니저 전용
    @GetMapping("/products/best-sales")
    public List<ProductResponse> getBestProducts(@AuthenticationPrincipal LoginMember member) {
        return productService.getBestProducts(member.getId());
    }

    // 판매량 WORST 3
    @GetMapping("/products/worst-sales")
    public List<ProductResponse> getWorstProducts(@AuthenticationPrincipal LoginMember member) {
        return productService.getWorstProducts(member.getId());
    }
}
