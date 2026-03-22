package org.example.blogspherebackend.repository;

import org.example.blogspherebackend.entity.Post;
import org.example.blogspherebackend.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

        Page<Post> findByStatusAndIsDeletedFalse(PostStatus status, Pageable pageable);

        Page<Post> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

        Optional<Post> findBySlugAndIsDeletedFalse(String slug);

        Optional<Post> findBySlug(String slug);

        boolean existsBySlug(String slug);

        @Query("SELECT p FROM Post p WHERE p.isDeleted = false " +
                "AND p.status = 'PUBLISHED' " +
                "AND LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
        Page<Post> searchPublishedPosts(@Param("searchTerm") String searchTerm, Pageable pageable);

        Page<Post> findByCategoryIdAndStatusAndIsDeletedFalse(Long categoryId, PostStatus status, Pageable pageable);

        Page<Post> findByTagsIdAndStatusAndIsDeletedFalse(Long tagId, PostStatus status, Pageable pageable);

        long countByCategoryId(Long categoryId);

        long countByCategoryIdAndStatusAndIsDeletedFalse(Long categoryId, PostStatus status);

        long countByUserIdAndIsDeletedFalse(Long userId);

        Page<Post> findByIsDeletedFalse(Pageable pageable);

        Page<Post> findByUserIdAndStatusAndIsDeletedFalse(Long userId, PostStatus status, Pageable pageable);

        @Modifying
        @Query("UPDATE Post p SET p.viewCount = p.viewCount + 1 WHERE p.id = :postId")
        void incrementViewCount(@Param("postId") Long postId);

        @Modifying
        @Query("UPDATE Post p SET p.likeCount = p.likeCount + 1 WHERE p.id = :postId")
        void incrementLikeCount(@Param("postId") Long postId);

        @Modifying
        @Query("UPDATE Post p SET p.likeCount = CASE WHEN p.likeCount > 0 THEN p.likeCount - 1 ELSE 0 END WHERE p.id = :postId")
        void decrementLikeCount(@Param("postId") Long postId);
}
