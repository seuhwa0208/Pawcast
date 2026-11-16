package com.project.powcast.product.controller;
import com.project.powcast.product.dto.*;
import com.project.powcast.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/products")
public class ProductController {
    private final ProductService productService;
    @GetMapping public List<ProductResponse> list(@Valid @ModelAttribute ProductSearchRequest request) { return productService.searchProducts(request); }
    @GetMapping("/{productId}/reviews") public ProductReviewsResponse reviews(@PathVariable Long productId) { return productService.reviews(productId); }
    @GetMapping("/best-reviews") public List<BestReviewProductResponse> bestReviews(@Valid @ModelAttribute ProductSearchRequest request) { return productService.bestReviewProducts(request); }
    @GetMapping("/{productId}") public ProductDetailResponse detail(@PathVariable Long productId) { return productService.detail(productId); }
}
