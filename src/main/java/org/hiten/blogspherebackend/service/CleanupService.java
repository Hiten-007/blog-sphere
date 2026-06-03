package org.hiten.blogspherebackend.service;

// Service interface for scheduled database cleanup operations
public interface CleanupService {

    void purgeDeletedPosts();

    void purgeDeletedUsers();

    void logDatabaseStats();

    void purgeOrphanImages();
}
