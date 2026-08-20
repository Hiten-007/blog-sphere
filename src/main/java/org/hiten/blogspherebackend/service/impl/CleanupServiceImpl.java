package org.hiten.blogspherebackend.service.impl;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.config.FileStorageConfig;
import org.hiten.blogspherebackend.repository.PostRepository;
import org.hiten.blogspherebackend.repository.UserRepository;
import org.hiten.blogspherebackend.service.CleanupService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Service
@Slf4j
@RequiredArgsConstructor
public class CleanupServiceImpl implements CleanupService {

    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final FileStorageConfig fileStorageConfig;

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

//    @Override
//    @Scheduled(cron = "${cleanup.cron.purge-orphan-images:0 0 3 * * SUN}")
//    @Transactional(readOnly = true)
//    public void purgeOrphanImages() {
//        long start = System.currentTimeMillis();
//        log.info("[CleanupJob] purgeOrphanImages starting");
//
//        Set<String> referencedPaths = new HashSet<>(postRepository.findAllFeaturedImageUrls());
//
//        Path uploadRoot = Paths.get(fileStorageConfig.getUploadDir()).toAbsolutePath().normalize();
//
//        if (!Files.exists(uploadRoot)) {
//            log.warn("[CleanupJob] purgeOrphanImages - upload root does not exist: {}", uploadRoot);
//            return;
//        }
//
//        int deletedCount = 0;
//        int errorCount = 0;
//
//        try (Stream<Path> fileStream = Files.walk(uploadRoot)) {
//            List<Path> allFiles = fileStream
//                    .filter(Files::isRegularFile)
//                    .toList();
//
//            log.info("[CleanupJob] purgeOrphanImages - scanning {} file(s) on disk", allFiles.size());
//
//            for (Path filePath : allFiles) {
//                String relativePath = uploadRoot.relativize(filePath).toString().replace("\\", "/");
//
//                if (!referencedPaths.contains(relativePath)) {
//                    try {
//                        Files.delete(filePath);
//                        deletedCount++;
//                        log.debug("[CleanupJob] purgeOrphanImages - deleted orphan: {}", relativePath);
//                    } catch (IOException e) {
//                        errorCount++;
//                        log.error("[CleanupJob] purgeOrphanImages - failed to delete {}: {}",
//                                relativePath, e.getMessage());
//                    }
//                }
//            }
//        } catch (IOException e) {
//            log.error("[CleanupJob] purgeOrphanImages - error walking upload directory: {}", e.getMessage(), e);
//        }
//
//        long duration = System.currentTimeMillis() - start;
//        log.info("[CleanupJob] purgeOrphanImages complete in {}ms - deleted={} errors={}",
//                duration, deletedCount, errorCount);
//    }
}
