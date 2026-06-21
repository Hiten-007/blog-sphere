package org.hiten.blogspherebackend.service;

import org.hiten.blogspherebackend.dto.request.CreateCommentRequest;
import org.hiten.blogspherebackend.dto.response.CommentResponse;
import org.springframework.data.domain.Page;

public interface CommentService {

    CommentResponse addComment(Long postId, Long userId, CreateCommentRequest request);

    void deleteComment(Long commentId, Long userId);

    Page<CommentResponse> getCommentsByPost(Long postId, int page, int size);

    CommentResponse getCommentById(Long commentId);

    Page<CommentResponse> getReplies(Long commentId, int page, int size);
}
