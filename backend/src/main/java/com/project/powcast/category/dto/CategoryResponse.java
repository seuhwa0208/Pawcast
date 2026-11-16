package com.project.powcast.category.dto;
import com.project.powcast.category.entity.Category;
public record CategoryResponse(Long id, Long parentId, String name) {
    public static CategoryResponse from(Category category) {
        return new CategoryResponse(category.getId(), category.getParent() == null ? null : category.getParent().getId(), category.getName());
    }
}
