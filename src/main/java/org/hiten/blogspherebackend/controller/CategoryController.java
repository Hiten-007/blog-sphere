package org.hiten.blogspherebackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.dto.request.CreateCategoryRequest;
import org.hiten.blogspherebackend.dto.response.CategoryResponse;
import org.hiten.blogspherebackend.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;


@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Slf4j
public class CategoryController {

    private final CategoryService categoryService;


    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> createCategory(
            @Valid @RequestBody CreateCategoryRequest request) {

        log.info("Creating category with name: '{}'", request.getName());

        CategoryResponse response = categoryService.createCategory(request);

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }


    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories() {

        log.info("Fetching all categories");

        List<CategoryResponse> categories = categoryService.getAllCategories();

        log.info("Returned {} categories", categories.size());

        return ResponseEntity.ok(categories);
    }


    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponse> getCategoryById(@PathVariable Long id) {

        log.info("Fetching category by ID: {}", id);

        CategoryResponse category = categoryService.getCategoryById(id);

        return ResponseEntity.ok(category);
    }


    @GetMapping("/slug/{slug}")
    public ResponseEntity<CategoryResponse> getCategoryBySlug(@PathVariable String slug) {

        log.info("Fetching category by slug: {}", slug);

        CategoryResponse category = categoryService.getCategoryBySlug(slug);

        return ResponseEntity.ok(category);
    }


    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CategoryResponse> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CreateCategoryRequest request) {

        log.info("Updating category with ID: {}", id);

        CategoryResponse response = categoryService.updateCategory(id, request);

        return ResponseEntity.ok(response);
    }


    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long id) {

        log.info("Deleting category with ID: {}", id);

        categoryService.deleteCategory(id);

        return ResponseEntity.noContent().build();
    }
}
