package org.hiten.blogspherebackend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AIConfig {

    /**
     * ChatClient backed by Gemini via OpenAI-compatible endpoint
     *
     * Spring AI 2.0.0 autoconfigures ChatClient.Builder from application.properties
     * (spring.ai.openai.*). We just call .build() here so all AI services can inject
     * a ready-to-use ChatCLient without knowing the underlying provider
     */
    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) { return builder.build(); }
}
