package org.hiten.blogspherebackend.mapper;

import org.hiten.blogspherebackend.dto.response.AuthorResponse;
import org.hiten.blogspherebackend.dto.response.CategoryResponse;
import org.hiten.blogspherebackend.dto.response.PostResponse;
import org.hiten.blogspherebackend.dto.response.TagResponse;
import org.hiten.blogspherebackend.entity.Post;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PostMapper {

    private final UserMapper userMapper;
    private final CategoryMapper categoryMapper;
    private final TagMapper tagMapper;

    @Autowired
    public PostMapper(UserMapper userMapper, CategoryMapper categoryMapper, TagMapper tagMapper) {
        this.userMapper = userMapper;
        this.categoryMapper = categoryMapper;
        this.tagMapper = tagMapper;
    }

    public PostResponse toResponse(Post post) {
        if(post == null) {
            return null;
        }
        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setSlug(post.getSlug());
        response.setContent(post.getContent());
        response.setExcerpt(post.getExcerpt());
        response.setFeaturedImageUrl(post.getFeaturedImageUrl());
        response.setStatus(post.getStatus());
        response.setViewCount(post.getViewCount());
        response.setLikeCount(post.getLikeCount());
        response.setPublishedAt(post.getPublishedAt());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());

        // WARNING: These calls trigger lazy loading!
        // Each one may execute a separate database query
        response.setAuthor(userMapper.toAuthorResponse(post.getUser()));
        response.setCategory(categoryMapper.toResponse(post.getCategory()));
        response.setTags(tagMapper.toResponseList(post.getTags()));

        return response;
    }

    public List<PostResponse> toResponseList(List<Post> posts) {
        return posts.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    public PostResponse toResponseWithoutRelations(Post post) {
        if(post == null) {
            return  null;
        }

        PostResponse response = new PostResponse();
        response.setId(post.getId());
        response.setTitle(post.getTitle());
        response.setSlug(post.getSlug());
        response.setContent(post.getContent());
        response.setExcerpt(post.getExcerpt());
        response.setFeaturedImageUrl(post.getFeaturedImageUrl());
        response.setStatus(post.getStatus());
        response.setViewCount(post.getViewCount());
        response.setLikeCount(post.getLikeCount());
        response.setPublishedAt(post.getPublishedAt());
        response.setCreatedAt(post.getCreatedAt());
        response.setUpdatedAt(post.getUpdatedAt());
        // author, category, tag skipped

        return response;
    }

    public static PostResponse toPostResponse(Post post) {
        if(post == null) {
            return null;
        }

        return PostResponse.builder()
                .id(post.getId())
                .title(post.getTitle())
                .slug(post.getSlug())
                .content(post.getContent())
                .excerpt(post.getExcerpt())
                .featuredImageUrl(post.getFeaturedImageUrl())
                .status(post.getStatus())
                .viewCount(post.getViewCount())
                .likeCount(post.getLikeCount())
                .author(AuthorResponse.fromEntity(post.getUser()))
                .category(CategoryResponse.fromEntity(post.getCategory()))
                .tags(post.getTags().stream()
                        .map(TagResponse::fromEntity)
                        .collect(Collectors.toList()))
                .publishedAt(post.getPublishedAt())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }
}
