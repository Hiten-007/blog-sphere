package org.hiten.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.dto.request.CreateCommentRequest;
import org.hiten.blogspherebackend.dto.response.CommentResponse;
import org.hiten.blogspherebackend.entity.Comment;
import org.hiten.blogspherebackend.entity.Post;
import org.hiten.blogspherebackend.entity.User;
import org.hiten.blogspherebackend.enums.Role;
import org.hiten.blogspherebackend.exception.ResourceNotFoundException;
import org.hiten.blogspherebackend.exception.UnauthorizedException;
import org.hiten.blogspherebackend.repository.CommentRepository;
import org.hiten.blogspherebackend.repository.PostRepository;
import org.hiten.blogspherebackend.repository.UserRepository;
import org.hiten.blogspherebackend.service.CommentService;
import org.hiten.blogspherebackend.service.EmailService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommentServiceImpl implements CommentService {

    private final CommentRepository commentRepository;
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Override
    @Transactional
    public CommentResponse addComment(Long postId, Long userId, CreateCommentRequest request) {
        Post post = postRepository.findById(postId)
                .filter(p -> !p.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("Post", "id", postId));

        User commenter = userRepository.findById(userId)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Comment parentComment = null;
        if (request.getParentCommentId() != null) {
            parentComment = commentRepository.findByIdAndIsDeletedFalse(request.getParentCommentId())
                    .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", request.getParentCommentId()));

            if (!parentComment.getPost().getId().equals(postId)) {
                throw new org.hiten.blogspherebackend.exception.BadRequestException(
                        "Parent comment does not belong to this post");
            }
        }

        Comment comment = Comment.builder()
                .post(post)
                .author(commenter)
                .parentComment(parentComment)
                .body(request.getBody())
                .isDeleted(false)
                .build();

        Comment saved = commentRepository.save(comment);

        if (parentComment == null) {
            log.info("Comment added to post {} by user: {}", postId, commenter.getEmail());
        } else {
            log.info("Reply added to comment {} (post {}) by user: {}",
                    parentComment.getId(), postId, commenter.getEmail());
        }

        boolean authorIsCommenter = post.getUser().getId().equals(userId);
        if (parentComment == null && !authorIsCommenter) {
            emailService.sendCommentNotification(post.getUser(), saved);
        }

        if (parentComment != null && !parentComment.getAuthor().getId().equals(userId)) {
            emailService.sendCommentNotification(parentComment.getAuthor(), saved);
        }

        return CommentResponse.fromEntity(saved);
    }

    @Override
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        User requestingUser = userRepository.findById(userId)
                .filter(u -> !u.getIsDeleted())
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        boolean isOwner = comment.getAuthor().getId().equals(userId);
        boolean isAdmin = requestingUser.getRole() == Role.ADMIN;

        if (!isOwner && !isAdmin) {
            throw new UnauthorizedException("You are not authorized to delete this comment");
        }
        commentRepository.softDeleteById(commentId);
        log.info("Comment {} soft-deleted by user: {}", commentId, requestingUser.getEmail());
    }

    @Override
    public Page<CommentResponse> getCommentsByPost(Long postId, int page, int size) {
        if (!postRepository.existsById(postId)) {
            throw new ResourceNotFoundException("Post", "id", postId);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        return commentRepository.findByPostIdAndParentCommentIsNullAndIsDeletedFalse(postId, pageable)
                .map(comment -> {
                    long replyCount = commentRepository.countByParentCommentIdAndIsDeletedFalse(comment.getId());
                    return CommentResponse.fromEntity(comment, replyCount);
                });
    }

    @Override
    public Page<CommentResponse> getReplies(Long commentId, int page, int size) {
        if (!commentRepository.findByIdAndIsDeletedFalse(commentId).isPresent()) {
            throw new ResourceNotFoundException("Comment", "id", commentId);
        }
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").ascending());

        return commentRepository.findByParentCommentIdAndIsDeletedFalse(commentId, pageable)
                .map(reply -> {
                    long replyCount = commentRepository.countByParentCommentIdAndIsDeletedFalse(reply.getId());
                    return CommentResponse.fromEntity(reply, replyCount);
                });
    }

    @Override
    public CommentResponse getCommentById(Long commentId) {
        Comment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment", "id", commentId));

        long replyCount = commentRepository.countByParentCommentIdAndIsDeletedFalse(commentId);
        return CommentResponse.fromEntity(comment, replyCount);
    }
}
