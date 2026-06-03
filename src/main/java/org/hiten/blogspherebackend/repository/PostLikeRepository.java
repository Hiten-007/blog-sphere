package org.hiten.blogspherebackend.repository;

import org.hiten.blogspherebackend.entity.PostLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PostLikeRepository extends JpaRepository<PostLike, Long> {

    Optional<PostLike> findByPostIdAndUserId(Long postId, Long userId);

    boolean existsByPostIdAndUserId(Long postId, Long userId);

    long countByPostId(Long postId);

    // Fetch all post IDs liked by a given user
    @Query("SELECT pl.post.id FROM PostLike pl WHERE pl.user.id = :userId")
    List<Long> findPostIdsByUserId(@Param("userId") Long userId);
}