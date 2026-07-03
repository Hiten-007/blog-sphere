package org.hiten.blogspherebackend.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hiten.blogspherebackend.dto.ai.*;
import org.hiten.blogspherebackend.entity.User;
import org.hiten.blogspherebackend.service.AIService;
import org.hiten.blogspherebackend.service.AIUsageService;
import org.hiten.blogspherebackend.service.ImageGenerationService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/v1/ai")
@RequiredArgsConstructor
public class AIController {

    private final AIService aiService;
    private final AIUsageService aiUsageService;
    private final ImageGenerationService imageGenerationService;

    @GetMapping("/test")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<GenerateOutlineResponse> testConnection() {
        log.info("AI connection test requested");
        GenerateOutlineRequest probe = new GenerateOutlineRequest();
        probe.setTopic("Spring AI");
        probe.setCategoryId(1L);
        return ResponseEntity.ok(aiService.generateOutline(probe));
    }

    @PostMapping("/outline")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenerateOutlineResponse> generateOutline(
            @Valid @RequestBody GenerateOutlineRequest request) {

        User user = currentUser();
        aiUsageService.checkAndIncrementUsage(user.getId(), user.getRole());

        return ResponseEntity.ok(aiService.generateOutline(request));
    }

    @PostMapping("/post")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GeneratePostResponse> generatePost(
            @Valid @RequestBody GeneratePostRequest request) {

        User user = currentUser();
        aiUsageService.checkAndIncrementUsage(user.getId(), user.getRole());

        return ResponseEntity.ok(aiService.generatePost(request));
    }

    @PostMapping("/expand")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ExpandContentResponse> expandContent(
            @Valid @RequestBody ExpandContentRequest request) {

        User user = currentUser();
        aiUsageService.checkAndIncrementUsage(user.getId(), user.getRole());

        return ResponseEntity.ok(aiService.expandContent(request));
    }

    @PostMapping("/excerpt")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, String>> generateExcerpt(
            @Valid @RequestBody ExcerptRequest request) {

        User user = currentUser();
        aiUsageService.checkAndIncrementUsage(user.getId(), user.getRole());

        return ResponseEntity.ok(Map.of("excerpt", aiService.generateExcerpt(request)));
    }

    @PostMapping("/suggest-tags")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Map<String, List<String>>> suggestTags(
            @Valid @RequestBody TagSuggestionRequest request) {

        User user = currentUser();
        aiUsageService.checkAndIncrementUsage(user.getId(), user.getRole());

        return ResponseEntity.ok(Map.of("tags", aiService.suggestTags(request)));
    }

    @PostMapping("/generate-image")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<GenerateImageResponse> generateImage(
            @Valid @RequestBody GenerateImageRequest request) {

        User user = currentUser();
        // Image generation counts as 2 uses — two AI calls (prompt agent + image model)
        aiUsageService.checkAndIncrementUsage(user.getId(), user.getRole());
        aiUsageService.checkAndIncrementUsage(user.getId(), user.getRole());

        return ResponseEntity.ok(imageGenerationService.generateFeaturedImage(request));
    }

    @GetMapping("/usage")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AIUsageResponse> getUsage() {
        User user = currentUser();
        return ResponseEntity.ok(aiUsageService.getUsage(user.getId(), user.getRole()));
    }

    private User currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (User) auth.getPrincipal();
    }
}

