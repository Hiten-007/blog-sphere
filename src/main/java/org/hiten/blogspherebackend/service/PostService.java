package org.hiten.blogspherebackend.service;

import org.hiten.blogspherebackend.dto.request.CreatePostRequest;
import org.hiten.blogspherebackend.dto.request.UpdatePostRequest;
import org.hiten.blogspherebackend.dto.response.PostResponse;
import org.hiten.blogspherebackend.dto.response.RestPage;
import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;


public interface PostService {


    PostResponse createPost(CreatePostRequest request, MultipartFile file, Long userId);


    PostResponse updatePost(Long postId, UpdatePostRequest request, Long userId);


    void deletePost(Long postId, Long userId);


    PostResponse getPostById(Long postId);


    PostResponse getPostBySlug(String slug);


    RestPage<PostResponse> getAllPosts(int page, int size, String sortBy, String sortDir);


    Page<PostResponse> getPostsByUser(Long userId, int page, int size);


    Page<PostResponse> searchPosts(String query, int page, int size);


    Page<PostResponse> getPostsByCategory(Long categoryId, int page, int size);


    List<PostResponse> getPostsByTag(Long tagId);


    void incrementViewCount(Long postId, String clientIp);


    PostResponse publishPost(Long postId, Long userId);

    PostResponse uploadFeaturedImage(Long postId, MultipartFile file, Long userId);
}

