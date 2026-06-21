package org.hiten.blogspherebackend.repository;

import org.hiten.blogspherebackend.entity.Comment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    @EntityGraph(attributePaths = {"author", "post", "post.user"})
    Page<Comment> findByPostIdAndIsDeletedFalse(Long postId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "post", "post.user"})
    Optional<Comment> findByIdAndIsDeletedFalse(Long id);

    long countByPostIdAndIsDeletedFalse(Long postId);

    long countByAuthorIdAndIsDeletedFalse(Long authorId);

    @Modifying
    @Query("UPDATE Comment c SET c.isDeleted = true WHERE c.id = :commentId")
    void softDeleteById(@Param("commentId") Long commentId);

    @EntityGraph(attributePaths = {"author", "post", "post.user"})
    Page<Comment> findByPostId(Long postId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "post", "post.user"})
    Page<Comment> findByPostIdAndParentCommentIsNullAndIsDeletedFalse(Long postId, Pageable pageable);

    @EntityGraph(attributePaths = {"author", "post", "post.user"})
    Page<Comment> findByParentCommentIdAndIsDeletedFalse(Long parentCommentId, Pageable pageable);

    long countByParentCommentIdAndIsDeletedFalse(Long parentCommentId);
}