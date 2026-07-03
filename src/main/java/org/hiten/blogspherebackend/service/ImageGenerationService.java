package org.hiten.blogspherebackend.service;

import org.hiten.blogspherebackend.dto.ai.GenerateImageRequest;
import org.hiten.blogspherebackend.dto.ai.GenerateImageResponse;

public interface ImageGenerationService {

    GenerateImageResponse generateFeaturedImage(GenerateImageRequest request);
}
