package org.hiten.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.entity.Post;
import org.hiten.blogspherebackend.entity.User;
import org.hiten.blogspherebackend.repository.PostRepository;
import org.hiten.blogspherebackend.repository.UserRepository;
import org.hiten.blogspherebackend.service.DigestService;
import org.hiten.blogspherebackend.service.EmailService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class DigestServiceImpl implements DigestService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    @Value("${cleanup.weekly-digest.top-posts:10}")
    private int weeklyDigestTopPosts;

    @Override
    @Scheduled(cron = "${cleanup.cron.weekly-digest:0 0 8 * * MON}")
    @Transactional(readOnly = true)
    public void sendWeeklyDigestEmails() {
        long start = System.currentTimeMillis();
        log.info("[DigestJob] sendWeeklyDigestEmails starting");

        LocalDateTime since = LocalDateTime.now().minusDays(7);
        List<Post> topPosts = postRepository.findTopEngagedPostsSince(
                since,
                PageRequest.of(0, weeklyDigestTopPosts)
        );

        if (topPosts.isEmpty()) {
            log.info("[DigestJob] sendWeeklyDigestEmails - no published posts in last 7 days, skipping digest");
            return;
        }

        log.info("[DigestJob] sendWeeklyDigestEmails - top {} post(s) selected for digest", topPosts.size());

        List<User> activeUsers = userRepository.findByEmailVerifiedTrueAndIsDeletedFalse();

        log.info("[DigestJob] sendWeeklyDigestEmails - sending digest to {} user(s)", activeUsers.size());

        int sentCount = 0;
        int errorCount = 0;

        for (User user : activeUsers) {
            try {
                emailService.sendWeeklyDigest(user, topPosts);
                sentCount++;
            } catch (Exception e) {
                errorCount++;
                log.error("[DigestJob] sendWeeklyDigestEmails - failed for userId={}: {}",
                        user.getId(), e.getMessage(), e);
            }
        }

        long duration = System.currentTimeMillis() - start;
        log.info("[DigestJob] sendWeeklyDigestEmails complete in {}ms - sent={} errors={}",
                duration, sentCount, errorCount);
    }
}