package com.project.powcast.product.service;

import com.project.powcast.product.dto.CategoryResponse;
import com.project.powcast.product.entity.Category;
import com.project.powcast.product.repository.CategoryRepository;
import com.project.powcast.common.exception.BusinessException;
import com.project.powcast.common.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository categoryRepository;

    public Category findById(Long id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND_CATEGORY));
    }

    public List<CategoryResponse> getCategories() {
        return categoryRepository.findAllByOrderByIdAsc().stream()
                .map(CategoryResponse::from)
                .toList();
    }
}
