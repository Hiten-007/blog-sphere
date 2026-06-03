package org.hiten.blogspherebackend.service;

import org.hiten.blogspherebackend.dto.response.LikeResponse;
import org.hiten.blogspherebackend.dto.response.PostResponse;

import java.util.List;

public interface PostLikeService {

    // Like a post — throws if already liked
    LikeResponse likePost(Long postId, Long userId);

    // Dislike (remove like) — only callable if post is currently liked
    LikeResponse dislikePost(Long postId, Long userId);

    // Get like status + count for a post from the perspective of a user
    LikeResponse getLikeStatus(Long postId, Long userId);

    // Get all posts liked by a user
    List<PostResponse> getLikedPostsByUser(Long userId);
}