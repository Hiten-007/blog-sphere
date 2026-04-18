package org.hiten.blogspherebackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.service.CategoryService;
import org.hiten.blogspherebackend.service.PostService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CacheWarmingConfig implements ApplicationRunner {

    private final PostService postService;
    private final CategoryService categoryService;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Cache warming started - pre-loading popular data into Redis");

        warmPostsCache();

        log.info("Cache warming completed!");
    }

    private void warmPostsCache() {
        try {
            postService.getAllPosts(0, 10, "publishedAt", "desc");
            log.info("Cache warming: first page of posts loaded (page=0, size=10)");
        } catch (Exception ex) {
            log.warn("Cache warming: failed to pre-load posts = {}", ex.getMessage());
        }
    }
}
