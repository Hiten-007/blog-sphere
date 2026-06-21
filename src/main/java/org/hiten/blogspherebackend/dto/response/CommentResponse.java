package org.hiten.blogspherebackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hiten.blogspherebackend.entity.Comment;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CommentResponse {

    private Long id;
    private String body;
    private AuthorResponse author;
    private Long postId;

    private Long parentCommentId;
    private long replyCount;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommentResponse fromEntity(Comment comment) {
        if (comment == null) {
            return null;
        }
        return CommentResponse.builder()
                .id(comment.getId())
                .body(comment.getBody())
                .author(AuthorResponse.fromEntity(comment.getAuthor()))
                .postId(comment.getPost().getId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getId() : null)
                .createdAt(comment.getCreatedAt())
                .updatedAt(comment.getUpdatedAt())
                .build();
    }

    public static CommentResponse fromEntity(Comment comment, long replyCount) {
        CommentResponse response = fromEntity(comment);
        if (response != null) {
            response.setReplyCount(replyCount);
        }
        return response;
    }
}
