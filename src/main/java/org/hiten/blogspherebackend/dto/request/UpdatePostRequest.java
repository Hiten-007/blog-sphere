package org.hiten.blogspherebackend.dto.request;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hiten.blogspherebackend.enums.PostStatus;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdatePostRequest {

    @Size(min = 5, max = 200, message = "Title must be between 5 and 200 characters")
    private String title;

    @Size(min = 50, message = "Content must be at least 50 characters")
    private String content;

    @Size(max = 500, message = "Excerpt cannot exceed 500 characters")
    private String excerpt;

    private String featuredImageUrl;

    private Long categoryId;

    private List<Long> tagIds;

    private PostStatus status;

    private LocalDateTime scheduledAt;
}
