package org.hiten.blogspherebackend.service;

public interface SchedulerService {

    // Auto-publish posts whose scheduledAt has passed
    void publishScheduledPosts();
}
