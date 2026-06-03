package org.hiten.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.entity.Post;
import org.hiten.blogspherebackend.enums.PostStatus;
import org.hiten.blogspherebackend.repository.PostRepository;
import org.hiten.blogspherebackend.repository.UserRepository;
import org.hiten.blogspherebackend.service.CleanupService;
import org.hiten.blogspherebackend.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class CleanupServiceImpl implements CleanupService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${cleanup.deleted-retention-days:30}")
    private int retentionDays;

    @Override
    @Scheduled(cron = "${cleanup.cron.purge-posts}")
    @Transactional
    public void purgeDeletedPosts() {
        long start = System.currentTimeMillis();
        log.info("[CleanupJob] purgeDeletedPosts starting - retention window: {} days", retentionDays);

        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = postRepository.hardDeleteByIsDeletedTrueAndUpdatedAtBefore(cutoff);

        long duration = System.currentTimeMillis() - start;
        log.info("[CleanupJob] purgeDeletedPosts complete = {} posts(s) hard-delete in {}ms", deleted, duration);
    }

    @Override
    @Scheduled(cron = "${cleanup.cron.purge-users}")
    @Transactional
    public void purgeDeletedUsers() {
        long start = System.currentTimeMillis();
        log.info("[CleanupJob] purgeDeletedUsers starting - retention window: {} days", retentionDays);

        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        int deleted = userRepository.hardDeleteByIsDeletedTrueAndUpdatedAtBefore(cutoff);

        long duration = System.currentTimeMillis() - start;
        log.info("[CleanupJob] purgeDeletedUsers complete = {} users(s) hard-delete in {}ms", deleted, duration);
    }

    /**
     * Log counts of soft-deleted records for operational monitoring.
     *
     * Fires every 6 hours. Uses COUNT queries only - no entities are loaded.
     * Output is visible in application logs so operators can set alerts if
     * soft-deleted counts grow unexpectedly
     */

    @Override
    @Scheduled(cron = "${cleanup.cron.stats}")
    public void logDatabaseStats() {
        long start = System.currentTimeMillis();
        log.info("[CleanupJob] logDatabaseStats starting");

        long softDeletedPosts = postRepository.countByIsDeletedTrue();
        long softDeletedUsers = userRepository.countByIsDeletedTrue();

        long duration = System.currentTimeMillis() - start;
        log.info("[CleanupJob] logDatabaseStats complete in {}ms - " +
                "soft-deleted posts: {}, soft-deleted users: {}",
                duration, softDeletedPosts, softDeletedUsers);
    }

    @Override
    @Scheduled(cron = "${cleanup.cron.publish-scheduled-posts:0 * * * * *}")
    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
    public void publishScheduledPosts() {
        long start = System.currentTimeMillis();
        log.info("[CleanupJob] publishScheduledPosts starting");

        List<Post> duePosts = postRepository.findScheduledPostsDue(LocalDateTime.now());

        if (duePosts.isEmpty()) {
            log.debug("[CleanupJob] publishScheduledPosts - no posts due for publishing");
            return;
        }

        log.info("[CleanupJob] publishScheduledPosts - found {} post(s) to publish", duePosts.size());

        for (Post post : duePosts) {
            post.setStatus(PostStatus.PUBLISHED);
            post.setPublishedAt(LocalDateTime.now());
            post.setScheduledAt(null);
            postRepository.save(post);

            log.info("[CleanupJob] publishScheduledPosts - published postId={} title=\"{}\" author={}",
                    post.getId(), post.getTitle(), post.getUser().getEmail());

            try {
                emailService.sendPostPublishedEmail(post.getUser(), post);
            } catch (Exception e) {
                log.error("[CleanupJob] publishScheduledPosts - failed to send publish email for postId={}: {}",
                        post.getId(), e.getMessage(), e);
            }
        }

        long duration = System.currentTimeMillis() - start;
        log.info("[CleanupJob] publishScheduledPosts complete - {} post(s) published in {}ms",
                duePosts.size(), duration);
    }
}
