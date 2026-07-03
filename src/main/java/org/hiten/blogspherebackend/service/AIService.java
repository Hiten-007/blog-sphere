package org.hiten.blogspherebackend.service;

import org.hiten.blogspherebackend.dto.ai.*;

import java.util.List;

public interface AIService {

    GenerateOutlineResponse generateOutline(GenerateOutlineRequest request);

    GeneratePostResponse generatePost(GeneratePostRequest request);

    ExpandContentResponse expandContent(ExpandContentRequest request);

    String generateExcerpt(ExcerptRequest request);

    List<String> suggestTags(TagSuggestionRequest request);
}

