package org.hiten.blogspherebackend.repository;

public interface SlugRepository {
    boolean existsBySlug(String slug);

    String getEntityName();
}
