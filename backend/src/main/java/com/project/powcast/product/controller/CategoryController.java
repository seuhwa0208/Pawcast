package com.project.powcast.product.controller;
import com.project.powcast.product.dto.CategoryResponse;
import com.project.powcast.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequiredArgsConstructor
public class CategoryController {
    private final ProductService productService;
    @GetMapping("/api/categories") public List<CategoryResponse> categories() { return productService.categories(); }
    @GetMapping("/api/brands") public List<String> brands() { return productService.brands(); }
}
