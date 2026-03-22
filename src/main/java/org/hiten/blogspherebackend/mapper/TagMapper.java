package org.hiten.blogspherebackend.mapper;

import org.hiten.blogspherebackend.dto.response.TagResponse;
import org.hiten.blogspherebackend.entity.Tag;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
public class TagMapper {

    public TagResponse toResponse(Tag tag) { return toTagResponse(tag); }

    public List<TagResponse> toResponseList(List<Tag> tags) {
        return tags.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public List<TagResponse> toResponseList(Set<Tag> tags) {
        return tags.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public static TagResponse toTagResponse(Tag tag) {
        if(tag == null) {
            return null;
        }
        return TagResponse.builder()
                .id(tag.getId())
                .name(tag.getName())
                .slug(tag.getSlug())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .build();
    }
}
