package com.example.qms.infrastructure.adapter;

import com.example.qms.application.port.out.IdempotencyHandlerPort;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;

@Component
class RedisIdempotencyHandlerAdapter implements IdempotencyHandlerPort {

    private final ReactiveStringRedisTemplate redisTemplate;

    public RedisIdempotencyHandlerAdapter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public CompletableFuture<Boolean> checkIdempotency(String key, Duration ttl) {
        String redisKey = "idempotency:" + key;
        return redisTemplate.opsForValue().setIfAbsent(redisKey, "1", ttl).toFuture();
    }
}
