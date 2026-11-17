package com.project.powcast.product.controller;

import com.project.powcast.product.dto.CategoryResponse;
import lombok.RequiredArgsConstructor;
import com.project.powcast.product.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/categories")
public class CategoryController {
    private final CategoryService categoryService;

    @GetMapping
    public List<CategoryResponse> getCategories() {
        return categoryService.getCategories();
    }
}
