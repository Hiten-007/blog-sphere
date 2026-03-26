package org.hiten.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.hiten.blogspherebackend.dto.request.CreateTagRequest;
import org.hiten.blogspherebackend.dto.response.TagResponse;
import org.hiten.blogspherebackend.entity.Tag;
import org.hiten.blogspherebackend.exception.BadRequestException;
import org.hiten.blogspherebackend.exception.ResourceNotFoundException;
import org.hiten.blogspherebackend.mapper.TagMapper;
import org.hiten.blogspherebackend.repository.TagRepository;
import org.hiten.blogspherebackend.service.TagService;
import org.hiten.blogspherebackend.util.SlugUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagServiceImpl implements TagService {

    private final TagRepository tagRepository;
    private final TagMapper tagMapper;

    @Override
    @Transactional
    public TagResponse createTag(CreateTagRequest request) {
        String normalizedName = normalizeTagName(request.getName());

        // Check if tag already exists
        if (tagRepository.existsByName(normalizedName)) {
            throw new BadRequestException("Tag with name '" + normalizedName + "' already exists");
        }

        // Ensure slug is unique
        String slug = SlugUtil.generateUniqueSlug(normalizedName, tagRepository);

        Tag tag = Tag.builder()
                .name(normalizedName)
                .slug(slug)
                .build();

        Tag savedTag = tagRepository.save(tag);

        return tagMapper.toResponse(savedTag);
    }

    @Override
    public List<TagResponse> getAllTags() {
        List<Tag> tags = tagRepository.findAllByOrderByNameAsc();

        return tags.stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public TagResponse getTagById(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));

        return tagMapper.toResponse(tag);
    }

    @Override
    public TagResponse getTagBySlug(String slug) {
        Tag tag = tagRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "slug", slug));

        return tagMapper.toResponse(tag);
    }

    @Override
    @Transactional
    public void deleteTag(Long id) {
        Tag tag = tagRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tag", "id", id));

        // Tags can be deleted even if posts use them
        // The post_tags junction table has ON DELETE CASCADE
        // Posts will simply lose this tag
        tagRepository.delete(tag);
    }

    @Override
    public List<TagResponse> searchTags(String searchTerm) {
        List<Tag> tags = tagRepository.searchByName(searchTerm);

        return tags.stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TagResponse> findTagsByPrefix(String prefix) {
        List<Tag> tags = tagRepository.findByNameStartingWith(prefix);

        return tags.stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public List<TagResponse> getOrCreateTags(Set<String> tagNames) {
        if (tagNames == null || tagNames.isEmpty()) {
            return Collections.emptyList();
        }

        // Normalize all tag names
        Set<String> normalizedNames = tagNames.stream()
                .map(this::normalizeTagName)
                .filter(name -> !name.isBlank())
                .collect(Collectors.toSet());

        // Find existing tags
        Set<Tag> existingTags = tagRepository.findByNameIn(normalizedNames);
        Set<String> existingNames = existingTags.stream()
                .map(Tag::getName)
                .collect(Collectors.toSet());

        // Create missing tags
        List<Tag> newTags = normalizedNames.stream()
                .filter(name -> !existingNames.contains(name))
                .map(name -> {
                    String slug = SlugUtil.generateUniqueSlug(name, tagRepository);
                    return Tag.builder()
                            .name(name)
                            .slug(slug)
                            .build();
                })
                .collect(Collectors.toList());

        // Save new tags
        List<Tag> savedNewTags = tagRepository.saveAll(newTags);

        // Combine existing and new tags
        List<Tag> allTags = new ArrayList<>(existingTags);
        allTags.addAll(savedNewTags);

        return allTags.stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public List<TagResponse> getPopularTags(int limit) {
        List<Tag> tags = tagRepository.findPopularTags(limit);

        return tags.stream()
                .map(tagMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    public boolean existsByName(String name) {
        return tagRepository.existsByName(normalizeTagName(name));
    }

    private String normalizeTagName(String name) {
        if (name == null) {
            return "";
        }
        return name.trim().toLowerCase(Locale.ENGLISH);
    }
}

