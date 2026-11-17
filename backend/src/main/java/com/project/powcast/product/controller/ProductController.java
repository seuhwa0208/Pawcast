package com.project.powcast.product.controller;

import com.project.powcast.product.dto.ProductDetailResponse;
import com.project.powcast.product.dto.ProductResponse;
import com.project.powcast.product.dto.ProductSearchRequest;
import com.project.powcast.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
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
}
