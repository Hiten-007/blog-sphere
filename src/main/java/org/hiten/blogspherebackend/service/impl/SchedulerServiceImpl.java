package org.hiten.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.entity.Post;
import org.hiten.blogspherebackend.enums.PostStatus;
import org.hiten.blogspherebackend.repository.PostRepository;
import org.hiten.blogspherebackend.service.EmailService;
import org.hiten.blogspherebackend.service.SchedulerService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class SchedulerServiceImpl implements SchedulerService {

    private final PostRepository postRepository;
    private final EmailService emailService;

    @Override
    @Scheduled(cron = "${cleanup.cron.publish-scheduled-posts:0 * * * * *}")
    @Transactional
    @CacheEvict(value = "posts", allEntries = true)
    public void publishScheduledPosts() {
        long start = System.currentTimeMillis();
        log.info("[SchedulerJob] publishScheduledPosts starting");

        List<Post> duePosts = postRepository.findScheduledPostsDue(LocalDateTime.now());

        if (duePosts.isEmpty()) {
            log.debug("[SchedulerJob] publishScheduledPosts - no posts due for publishing");
            return;
        }

        log.info("[SchedulerJob] publishScheduledPosts - found {} post(s) to publish", duePosts.size());

        for (Post post : duePosts) {
            post.setStatus(PostStatus.PUBLISHED);
            post.setPublishedAt(LocalDateTime.now());
            post.setScheduledAt(null);
            postRepository.save(post);

            log.info("[SchedulerJob] publishScheduledPosts - published postId={} title=\"{}\" author={}",
                    post.getId(), post.getTitle(), post.getUser().getEmail());

            try {
                emailService.sendPostPublishedEmail(post.getUser(), post);
            } catch (Exception e) {
                log.error("[SchedulerJob] publishScheduledPosts - failed to send publish email for postId={}: {}",
                        post.getId(), e.getMessage(), e);
            }
        }

        long duration = System.currentTimeMillis() - start;
        log.info("[SchedulerJob] publishScheduledPosts complete - {} post(s) published in {}ms",
                duePosts.size(), duration);
    }
}