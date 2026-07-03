package org.hiten.blogspherebackend.dto.ai;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TagSuggestionRequest {

    @NotBlank
    private String content;

    private int maxTags;
}

