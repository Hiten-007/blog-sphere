package org.hiten.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.hiten.blogspherebackend.dto.request.CreateCategoryRequest;
import org.hiten.blogspherebackend.dto.response.CategoryResponse;
import org.hiten.blogspherebackend.entity.Category;
import org.hiten.blogspherebackend.exception.BadRequestException;
import org.hiten.blogspherebackend.exception.ResourceNotFoundException;
import org.hiten.blogspherebackend.mapper.CategoryMapper;
import org.hiten.blogspherebackend.repository.CategoryRepository;
import org.hiten.blogspherebackend.repository.PostRepository;
import org.hiten.blogspherebackend.service.CategoryService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final PostRepository postRepository;
    private final CategoryMapper categoryMapper;

    private static final Pattern NON_LATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");
    private static final Pattern MULTIPLE_HYPHENS = Pattern.compile("-+");

    @Override
    @Transactional
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        // Check if name already exists
        if (categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        // Generate slug from name
        String slug = generateSlug(request.getName());

        // Ensure slug is unique
        slug = ensureUniqueSlug(slug, null);

        Category category = Category.builder()
                .name(request.getName())
                .slug(slug)
                .description(request.getDescription())
                .build();

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    public CategoryResponse updateCategory(Long id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Check if new name conflicts with another category
        if (!category.getName().equals(request.getName()) &&
                categoryRepository.existsByName(request.getName())) {
            throw new BadRequestException("Category with name '" + request.getName() + "' already exists");
        }

        // Update name and regenerate slug if name changed
        if (!category.getName().equals(request.getName())) {
            category.setName(request.getName());
            String newSlug = generateSlug(request.getName());
            category.setSlug(ensureUniqueSlug(newSlug, category.getSlug()));
        }

        // Update description
        if (request.getDescription() != null) {
            category.setDescription(request.getDescription());
        }

        Category savedCategory = categoryRepository.save(category);

        return categoryMapper.toResponse(savedCategory);
    }

    @Override
    @Transactional
    public void deleteCategory(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        // Check if any posts use this category
        long postCount = postRepository.countByCategoryId(id);
        if (postCount > 0) {
            throw new BadRequestException(
                    "Cannot delete category '" + category.getName() +
                            "'. It is used by " + postCount + " post(s). " +
                            "Please reassign or delete those posts first.");
        }

        categoryRepository.delete(category);
    }

    @Override
    public CategoryResponse getCategoryById(Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", id));

        return categoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse getCategoryBySlug(String slug) {
        Category category = categoryRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Category", "slug", slug));

        return categoryMapper.toResponse(category);
    }

    @Override
    public List<CategoryResponse> getAllCategories() {
        List<Category> categories = categoryRepository.findAllByOrderByNameAsc();

        return categories.stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<CategoryResponse> searchCategories(String searchTerm) {
        List<Category> categories = categoryRepository.searchByName(searchTerm);

        return categories.stream()
                .map(categoryMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByName(String name) {
        return categoryRepository.existsByName(name);
    }


    private String generateSlug(String name) {
        if (name == null || name.isBlank()) {
            return "";
        }

        String normalized = Normalizer.normalize(name, Normalizer.Form.NFD);
        String slug = WHITESPACE.matcher(normalized).replaceAll("-");
        slug = NON_LATIN.matcher(slug).replaceAll("");
        slug = MULTIPLE_HYPHENS.matcher(slug).replaceAll("-");
        slug = slug.toLowerCase(Locale.ENGLISH);
        slug = slug.replaceAll("^-|-$", "");

        return slug;
    }


    private String ensureUniqueSlug(String baseSlug, String currentSlug) {
        if (baseSlug.isEmpty()) {
            baseSlug = "category";
        }

        // If generated slug matches current slug, keep it
        if (baseSlug.equals(currentSlug)) {
            return currentSlug;
        }

        String slug = baseSlug;
        int counter = 1;

        while (categoryRepository.existsBySlug(slug) && !slug.equals(currentSlug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }

        return slug;
    }
}
