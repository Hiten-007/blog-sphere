package org.hiten.blogspherebackend.repository;

import org.hiten.blogspherebackend.entity.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends SlugRepository, JpaRepository<Category, Long> {

    Optional<Category> findBySlug(String slug);

    Optional<Category> findByName(String name);

    boolean existsByName(String name);

    boolean existsBySlug(String slug);

    List<Category> findAllByOrderByNameAsc();

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT('%', :searchTerm, '%'))")
    List<Category> searchByName(@Param("searchTerm") String searchTerm);

    @Query("SELECT c FROM Category c WHERE LOWER(c.name) LIKE LOWER(CONCAT(:prefix, '%')) ORDER BY c.name")
    List<Category> findByNameStartingWith(@Param("prefix") String prefix);

    default String getEntityName() {
        return "category";
    }
}
