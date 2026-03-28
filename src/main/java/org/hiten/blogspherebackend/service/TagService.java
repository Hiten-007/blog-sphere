package org.hiten.blogspherebackend.service;

import org.hiten.blogspherebackend.dto.request.CreateTagRequest;
import org.hiten.blogspherebackend.dto.response.TagResponse;

import java.util.List;
import java.util.Set;


public interface TagService {

    TagResponse createTag(CreateTagRequest request);

    List<TagResponse> getAllTags();

    TagResponse getTagById(Long id);

    TagResponse getTagBySlug(String slug);

    void deleteTag(Long id);

    List<TagResponse> searchTags(String searchTerm);

    List<TagResponse> findTagsByPrefix(String prefix);

    List<TagResponse> getOrCreateTags(Set<String> tagNames);

    List<TagResponse> getPopularTags(int limit);

    boolean existsByName(String name);

    TagResponse updateTag(Long id, CreateTagRequest request);
}

