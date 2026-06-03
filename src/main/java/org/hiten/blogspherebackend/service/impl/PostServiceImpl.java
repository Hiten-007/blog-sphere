package org.hiten.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import org.hiten.blogspherebackend.dto.request.CreatePostRequest;
import org.hiten.blogspherebackend.dto.request.UpdatePostRequest;
import org.hiten.blogspherebackend.dto.response.PostResponse;
import org.hiten.blogspherebackend.dto.response.RestPage;
import org.hiten.blogspherebackend.entity.Category;
import org.hiten.blogspherebackend.entity.Post;
import org.hiten.blogspherebackend.entity.Tag;
import org.hiten.blogspherebackend.entity.User;
import org.hiten.blogspherebackend.enums.PostStatus;
import org.hiten.blogspherebackend.enums.Role;
import org.hiten.blogspherebackend.exception.BadRequestException;
import org.hiten.blogspherebackend.exception.ResourceNotFoundException;
import org.hiten.blogspherebackend.exception.UnauthorizedException;
import org.hiten.blogspherebackend.mapper.PostMapper;
import org.hiten.blogspherebackend.repository.CategoryRepository;
import org.hiten.blogspherebackend.repository.PostRepository;
import org.hiten.blogspherebackend.repository.TagRepository;
import org.hiten.blogspherebackend.repository.UserRepository;
import org.hiten.blogspherebackend.service.FileStorageService;
import org.hiten.blogspherebackend.service.PostService;
import org.hiten.blogspherebackend.util.SlugUtil;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostServiceImpl implements PostService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final PostMapper postMapper;
    private final FileStorageService fileStorageService;

    @Override
    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
    public PostResponse createPost(CreatePostRequest request, MultipartFile file, Long userId) {
        // Find the author
        User author = userRepository.findById(userId)
                .filter(user -> !user.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // Find the category
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));

        // Find tags if provided
        Set<Tag> tags = new HashSet<>();
        if (request.getTagIds() != null && !request.getTagIds().isEmpty()) {
            tags = new HashSet<>(tagRepository.findAllById(request.getTagIds()));
        }

        // Generate unique slug from title
        String slug = SlugUtil.generateUniqueSlug(request.getTitle(), postRepository);

        String featuredImageUrl = request.getFeaturedImageUrl();
        if(file != null && !file.isEmpty()) {
            featuredImageUrl = fileStorageService.storeFile(file, "posts");
        }

        // Create the post
        Post post = Post.builder()
                .user(author)
                .category(category)
                .title(request.getTitle())
                .slug(slug)
                .content(request.getContent())
                .excerpt(request.getExcerpt())
                .featuredImageUrl(featuredImageUrl)
                .status(request.getStatus() != null ? request.getStatus() : PostStatus.DRAFT)
                .tags(tags)
                .build();

        // Set publishedAt if publishing immediately
        if (post.getStatus() == PostStatus.PUBLISHED) {
            post.setPublishedAt(LocalDateTime.now());
        }

        // scheduledAt must be provided and must be in the future
        if (post.getStatus() == PostStatus.SCHEDULED) {
            if (request.getScheduledAt() == null) {
                throw new BadRequestException("scheduledAt is required when status is SCHEDULED");
            }
            if (request.getScheduledAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("scheduledAt must be in the future");
            }
            post.setScheduledAt(request.getScheduledAt());
        }

        Post savedPost = postRepository.save(post);

        return postMapper.toResponse(savedPost);
    }

    @Override
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public PostResponse updatePost(Long postId, UpdatePostRequest request, Long userId) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Check if user is the author
        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to update this post");
        }

        // Update title and regenerate slug if title changed
        if (request.getTitle() != null) {
            post.setTitle(request.getTitle());
            String newSlug = SlugUtil.generateUniqueSlug(request.getTitle(), post.getSlug(), postRepository);
            post.setSlug(newSlug);
        }

        // Update content
        if (request.getContent() != null) {
            post.setContent(request.getContent());
        }

        // Update excerpt
        if (request.getExcerpt() != null) {
            post.setExcerpt(request.getExcerpt());
        }

        // Update featured image
        if (request.getFeaturedImageUrl() != null) {
            post.setFeaturedImageUrl(request.getFeaturedImageUrl());
        }

        // Update category
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", "id", request.getCategoryId()));
            post.setCategory(category);
        }

        // Update tags
        if (request.getTagIds() != null) {
            Set<Tag> tags = new HashSet<>(tagRepository.findAllById(request.getTagIds()));
            post.setTags(tags);
        }

        // Update status
        if (request.getStatus() != null) {
            PostStatus oldStatus = post.getStatus();
            post.setStatus(request.getStatus());

            // Set publishedAt when publishing for the first time
            if (oldStatus != PostStatus.PUBLISHED && request.getStatus() == PostStatus.PUBLISHED) {
                post.setPublishedAt(LocalDateTime.now());
                post.setScheduledAt(null);
            }

            // Handle scheduling
            if (request.getStatus() == PostStatus.SCHEDULED) {
                if (request.getScheduledAt() == null) {
                    throw new BadRequestException("scheduledAt is required when status is SCHEDULED");
                }
                if (request.getScheduledAt().isBefore(LocalDateTime.now())) {
                    throw new BadRequestException("scheduledAt must be in the future");
                }
                post.setScheduledAt(request.getScheduledAt());
            }

            // Unscheduling (moving back to DRAFT)
            if (request.getStatus() == PostStatus.DRAFT && oldStatus == PostStatus.SCHEDULED) {
                post.setScheduledAt(null);
            }
        }

        Post savedPost = postRepository.save(post);

        return postMapper.toResponse(savedPost);
    }

    @Override
    @Transactional
    @CacheEvict(value = "posts", key = "#postId")
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Check if user is the author
        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to delete this post");
        }

        // Soft delete
        post.setIsDeleted(true);
        post.setDeletedAt(LocalDateTime.now());
        postRepository.save(post);
    }

    @Override
    @Cacheable(value = "posts", key = "#postId")
    public PostResponse getPostById(Long postId) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        return postMapper.toResponse(post);
    }

    @Override
    @Cacheable(value = "posts", key = "#slug")
    public PostResponse getPostBySlug(String slug) {
        Post post = postRepository.findBySlugAndIsDeletedFalse(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Post", "slug", slug));

        return postMapper.toResponse(post);
    }

    @Override
    @Cacheable(value = "posts", key = "'all_' + #page + '_' + #size + '_' + #sortBy + '_' + #sortDir", condition = "#page == 0")
    public RestPage<PostResponse> getAllPosts(int page, int size, String sortBy, String sortDir) {
        Sort sort = sortDir.equalsIgnoreCase("asc")
                ? Sort.by(sortBy).ascending()
                : Sort.by(sortBy).descending();

        Pageable pageable = PageRequest.of(page, size, sort);

        Page<Post> posts = postRepository.findByStatusAndIsDeletedFalse(
                PostStatus.PUBLISHED,
                pageable
        );

        // Wrapped in RestPage so Jackson can deserialize it on cache read-back
        return new RestPage<>(posts.map(postMapper::toResponse));
    }

    @Override
    public Page<PostResponse> getPostsByUser(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Post> posts = postRepository.findByUserIdAndIsDeletedFalse(userId, pageable);

        return posts.map(postMapper::toResponse);
    }

    @Override
    public Page<PostResponse> searchPosts(String query, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        // Step 1: GIN index scan -> returns a page of matching IDs with total count metadata
        Page<Long> idPage =postRepository.findPublishedPostIdsByFullTextSearch(query, pageable);

        if(idPage.isEmpty()) {
            return Page.empty(pageable);
        }

        // Step 2: load full entities with associations for this page of IDs
        // @EntityGraph in repository ensures user, category, tags are fetched
        // in a single JOIN FETCH - no N+1 problem
        List<Post> posts = postRepository.findByIdIn(idPage.getContent());

        // Re-apply the Step 1 ordering (IN clause in JPQL does not preserve order)
        Map<Long, Post> byId = posts.stream()
                .collect(Collectors.toMap(Post::getId, p -> p));
        List<PostResponse> ordered = idPage.getContent().stream()
                .map(byId::get)
                .filter(Objects::nonNull)
                .map(postMapper::toResponse)
                .collect(Collectors.toList());

        return new PageImpl<>(ordered, pageable, idPage.getTotalElements());
    }

    @Override
    @Cacheable(value = "posts", key = "'all_' + #categoryId + '_' + #page + '_' + #size")
    public Page<PostResponse> getPostsByCategory(Long categoryId, int page, int size) {
        // Verify category exists
        if (!categoryRepository.existsById(categoryId)) {
            throw new ResourceNotFoundException("Category", "id", categoryId);
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by("publishedAt").descending());

        Page<Post> posts = postRepository.findByCategoryIdAndStatusAndIsDeletedFalse(
                categoryId,
                PostStatus.PUBLISHED,
                pageable
        );

        return posts.map(postMapper::toResponse);
    }

    @Override
    public List<PostResponse> getPostsByTag(Long tagId) {
        // Verify tag exists
        if (!tagRepository.existsById(tagId)) {
            throw new ResourceNotFoundException("Tag", "id", tagId);
        }

        List<Post> posts = postRepository.findByTagsIdAndStatusAndIsDeletedFalse(
                tagId,
                PostStatus.PUBLISHED
        );

        return posts.stream()
                .map(postMapper::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void incrementViewCount(Long postId) {
        // Check if post exists
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        // Atomic increment - no N+1 problem here!
        postRepository.incrementViewCount(postId);
    }

    @Override
    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
    public PostResponse publishPost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // Check if user is the author
        if (!post.getUser().getId().equals(userId)) {
            throw new UnauthorizedException("You are not authorized to publish this post");
        }

        // Set status and publish date
        post.setStatus(PostStatus.PUBLISHED);
        if (post.getPublishedAt() == null) {
            post.setPublishedAt(LocalDateTime.now());
        }

        Post savedPost = postRepository.save(post);

        return postMapper.toResponse(savedPost);
    }

    @Override
    @Transactional
    public PostResponse uploadFeaturedImage(Long postId, MultipartFile file, Long userId) {
        // 1. Find post
        Post post = postRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        // 2. Find requesting user
        User user = userRepository.findById(userId)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 3. Verify ownership or ADMIN role
        boolean isOwner = post.getUser().getId().equals(userId);
        boolean isAdmin = user.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to upload an image for this post");
        }

        // 4. Delete old featured image if present
        if (post.getFeaturedImageUrl() != null && !post.getFeaturedImageUrl().isBlank()) {
            fileStorageService.deleteFile(post.getFeaturedImageUrl());
        }

        // 5. Store new file
        String newImagePath = fileStorageService.storeFile(file, "posts");

        // 6. Update and save
        post.setFeaturedImageUrl(newImagePath);
        Post savedPost = postRepository.save(post);

        return postMapper.toResponse(savedPost);
    }
}

