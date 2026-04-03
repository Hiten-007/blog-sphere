package org.hiten.blogspherebackend.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Configuration
@EnableCaching
@RequiredArgsConstructor
public class RedisCacheConfig {

    private final CacheTtlProperties ttl;

    private GenericJacksonJsonRedisSerializer jacksonSerializer() {
        return GenericJacksonJsonRedisSerializer.builder()
                .enableUnsafeDefaultTyping()
                .build();
    }

    private RedisCacheConfiguration defaultCacheConfiguration() {
        return RedisCacheConfiguration.defaultCacheConfig()
                .entryTtl(Duration.ofSeconds(ttl.getDefaultTtl()))
                .disableCachingNullValues()
                .serializeKeysWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(new StringRedisSerializer())
                )
                .serializeValuesWith(
                        RedisSerializationContext.SerializationPair
                                .fromSerializer(jacksonSerializer())
                );
    }

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory connectionFactory) {
        log.info("Initialising RedisCacheManager - posts={}s, categories={}s, tags={}s, default={}s",
                ttl.getPosts(), ttl.getCategories(), ttl.getTags(), ttl.getDefaultTtl());

        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        cacheConfigurations.put("posts",
                defaultCacheConfiguration().entryTtl(Duration.ofSeconds(ttl.getPosts())));

        cacheConfigurations.put("categories",
                defaultCacheConfiguration().entryTtl(Duration.ofSeconds(ttl.getCategories())));

        cacheConfigurations.put("tags",
                defaultCacheConfiguration().entryTtl(Duration.ofSeconds(ttl.getTags())));

        return RedisCacheManager.builder(connectionFactory)
                .cacheDefaults(defaultCacheConfiguration())
                .withInitialCacheConfigurations(cacheConfigurations)
                .build();
    }
}
