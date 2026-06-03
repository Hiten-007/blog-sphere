package org.hiten.blogspherebackend.repository;

import org.hiten.blogspherebackend.entity.Post;
import org.hiten.blogspherebackend.enums.PostStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface PostRepository extends SlugRepository, JpaRepository<Post, Long> {

        @EntityGraph(attributePaths = {"user", "category", "tags"})
        Page<Post> findByStatusAndIsDeletedFalse(PostStatus status, Pageable pageable);

        @EntityGraph(attributePaths = {"user", "category", "tags"})
        Page<Post> findByUserIdAndIsDeletedFalse(Long userId, Pageable pageable);

        Optional<Post> findBySlugAndIsDeletedFalse(String slug);

        Optional<Post> findBySlug(String slug);

        boolean existsBySlug(String slug);

        @Query(
                value = "SELECT p.id FROM posts p " +
                        "WHERE p.is_deleted = false " +
                        "AND p.status = 'PUBLISHED' " +
                        "AND p.search_vector @@ plainto_tsquery('english', :query) " +
                        "ORDER BY p.published_at DESC",
                countQuery = "SELECT COUNT(p.id) FROM posts p " +
                        "WHERE p.is_deleted = false " +
                        "AND p.status = 'PUBLISHED' " +
                        "AND p.search_vector @@ plainto_tsquery('english', :query)",
                nativeQuery = true
        )
        Page<Long> findPublishedPostIdsByFullTextSearch(@Param("query") String query, Pageable pageable);

        @EntityGraph(attributePaths = {"user", "category", "tags"})
        @Query("SELECT p FROM Post p WHERE p.id IN :ids")
        List<Post> findByIdIn(@Param("ids") List<Long> ids);

        @EntityGraph(attributePaths = {"user", "category", "tags"})
        @Query("SELECT p FROM Post p WHERE p.isDeleted = false " +
                "AND p.status = 'PUBLISHED' " +
                "AND LOWER(p.title) LIKE LOWER(CONCAT('%', :searchTerm, '%')) " +
                "OR LOWER(p.content) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
        Page<Post> searchPublishedPosts(@Param("searchTerm") String searchTerm, Pageable pageable);

        @EntityGraph(attributePaths = {"user", "category", "tags"})
        Page<Post> findByCategoryIdAndStatusAndIsDeletedFalse(Long categoryId, PostStatus status, Pageable pageable);

        @EntityGraph(attributePaths = {"user", "category", "tags"})
        Page<Post> findByTagsIdAndStatusAndIsDeletedFalse(Long tagId, PostStatus status, Pageable pageable);

        @EntityGraph(attributePaths = {"user", "category", "tags"})
        List<Post> findByTagsIdAndStatusAndIsDeletedFalse(Long tagId, PostStatus status);

        long countByCategoryId(Long categoryId);

        long countByCategoryIdAndStatusAndIsDeletedFalse(Long categoryId, PostStatus status);

        long countByUserIdAndIsDeletedFalse(Long userId);

        @EntityGraph(attributePaths = {"user", "category", "tags"})
        Page<Post> findByIsDeletedFalse(Pageable pageable);

        @EntityGraph(attributePaths = {"user", "category", "tags"})
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

        default String getBaseSlug() {
                return "post";
        }

        @Modifying
        @Query("DELETE FROM Post p WHERE p.isDeleted = true AND p.updatedAt < :cutoff")
        int hardDeleteByIsDeletedTrueAndUpdatedAtBefore(@Param("cutoff") LocalDateTime cutoff);

        long countByIsDeletedTrue();

        @Modifying
        @Query("UPDATE Post p SET p.updatedAt = :updatedAt WHERE p.id = :id")
        void backdateUpdatedAt(@Param("id") Long id, @Param("updatedAt") LocalDateTime updatedAt);

        @Modifying
        @Query("DELETE FROM Post p WHERE p.isDeleted = true AND p.deletedAt < :cutoff")
        int hardDeleteByIsDeletedTrueAndDeletedAtBefore(@Param("cutoff") LocalDateTime cutoff);

        // Used by the scheduler to auto-publish posts whose scheduledAt has passed
        @EntityGraph(attributePaths = {"user", "category", "tags"})
        @Query("SELECT p FROM Post p WHERE p.status = 'SCHEDULED' AND p.scheduledAt <= :now AND p.isDeleted = false")
        List<Post> findScheduledPostsDue(@Param("now") LocalDateTime now);
}
