package org.hiten.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.dto.response.LikeResponse;
import org.hiten.blogspherebackend.dto.response.PostResponse;
import org.hiten.blogspherebackend.entity.Post;
import org.hiten.blogspherebackend.entity.PostLike;
import org.hiten.blogspherebackend.entity.User;
import org.hiten.blogspherebackend.exception.BadRequestException;
import org.hiten.blogspherebackend.exception.ResourceNotFoundException;
import org.hiten.blogspherebackend.mapper.PostMapper;
import org.hiten.blogspherebackend.repository.PostLikeRepository;
import org.hiten.blogspherebackend.repository.PostRepository;
import org.hiten.blogspherebackend.repository.UserRepository;
import org.hiten.blogspherebackend.service.PostLikeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PostLikeServiceImpl implements PostLikeService {

    private final PostLikeRepository postLikeRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostMapper postMapper;

    @Override
    @Transactional
    public LikeResponse likePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        User user = userRepository.findById(userId)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        if (postLikeRepository.existsByPostIdAndUserId(postId, userId)) {
            throw new BadRequestException("You have already liked this post");
        }

        PostLike like = PostLike.builder()
                .post(post)
                .user(user)
                .build();
        postLikeRepository.save(like);

        postRepository.incrementLikeCount(postId);

        long likeCount = postLikeRepository.countByPostId(postId);
        log.info("[Like] userId={} liked postId={} — total likes now {}", userId, postId, likeCount);

        return LikeResponse.builder()
                .postId(postId)
                .likeCount(likeCount)
                .likedByCurrentUser(true)
                .build();
    }

    @Override
    @Transactional
    public LikeResponse dislikePost(Long postId, Long userId) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        PostLike like = postLikeRepository.findByPostIdAndUserId(postId, userId)
                .orElseThrow(() -> new BadRequestException("You have not liked this post"));

        postLikeRepository.delete(like);

        postRepository.decrementLikeCount(postId);

        long likeCount = postLikeRepository.countByPostId(postId);
        log.info("[Like] userId={} removed like from postId={} — total likes now {}", userId, postId, likeCount);

        return LikeResponse.builder()
                .postId(postId)
                .likeCount(likeCount)
                .likedByCurrentUser(false)
                .build();
    }

    @Override
    public LikeResponse getLikeStatus(Long postId, Long userId) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }

        long likeCount = postLikeRepository.countByPostId(postId);
        boolean liked = postLikeRepository.existsByPostIdAndUserId(postId, userId);

        return LikeResponse.builder()
                .postId(postId)
                .likeCount(likeCount)
                .likedByCurrentUser(liked)
                .build();
    }

    @Override
    public List<PostResponse> getLikedPostsByUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", "id", userId);
        }

        List<Long> likedPostIds = postLikeRepository.findPostIdsByUserId(userId);

        if (likedPostIds.isEmpty()) {
            return List.of();
        }

        return postRepository.findByIdIn(likedPostIds)
                .stream()
                .filter(p -> !p.getIsDeleted())
                .map(postMapper::toResponse)
                .toList();
    }
}