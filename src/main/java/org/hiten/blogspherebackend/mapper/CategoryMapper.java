package org.hiten.blogspherebackend.mapper;

import org.hiten.blogspherebackend.dto.response.CategoryResponse;
import org.hiten.blogspherebackend.entity.Category;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {

    public CategoryResponse toResponse(Category category) {
        return toCategoryResponse(category);
    }

    public List<CategoryResponse> toResponseList(List<Category> categories) {
        return categories.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    // Static method to convert Category to CategoryResponse. Can be used without Spring context
    public static CategoryResponse toCategoryResponse(Category category) {
        if(category == null) {
            return null;
        }
        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .build();
    }
}
