package org.hiten.blogspherebackend.service;

import org.hiten.blogspherebackend.dto.ai.AIUsageResponse;
import org.hiten.blogspherebackend.enums.Role;

public interface AIUsageService {

    void checkAndIncrementUsage(Long userId, Role role);

    AIUsageResponse getUsage(Long userId, Role role);
}

