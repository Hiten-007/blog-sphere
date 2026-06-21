package org.hiten.blogspherebackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateCommentRequest {

    @NotBlank(message = "Comment body is required")
    @Size(min = 1, max = 2000, message = "Comment must be between 1 and 2000 characters")
    private String body;

    private Long parentCommentId;
}
