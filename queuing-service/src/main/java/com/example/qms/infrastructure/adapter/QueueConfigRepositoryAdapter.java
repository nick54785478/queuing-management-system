package com.example.qms.infrastructure.adapter;

import com.example.qms.application.port.out.QueueConfigRepositoryPort;
import com.example.qms.domain.config.aggregate.root.QueueConfig;
import com.example.qms.domain.config.aggregate.vo.QueueMode;
import com.example.qms.infrastructure.persistence.config.entity.QueueConfigJpaEntity;
import com.example.qms.infrastructure.persistence.config.repository.QueueConfigJpaRepository;
import com.example.qms.infrastructure.persistence.entity.QueueConfigRedisEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.CompletableFuture;

@Component
class QueueConfigRepositoryAdapter implements QueueConfigRepositoryPort {

    private final QueueConfigJpaRepository jpaRepository;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public QueueConfigRepositoryAdapter(QueueConfigJpaRepository jpaRepository,
                                        ReactiveStringRedisTemplate redisTemplate,
                                        ObjectMapper objectMapper) {
        this.jpaRepository = jpaRepository;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    private String getRedisKey(String tenantId, String activityId) {
        return "qms:config:tenant:" + tenantId + ":activity:" + activityId;
    }

    @Override
    public CompletableFuture<QueueConfig> save(QueueConfig config) {
        // 1. Save to JPA blocking repository (wrapped in boundedElastic to avoid blocking EventLoop)
        return Mono.fromCallable(() -> {
                    QueueConfigJpaEntity entity = jpaRepository.findByTenantIdAndActivityId(config.getTenantId(), config.getActivityId())
                            .orElseGet(() -> new QueueConfigJpaEntity(config.getTenantId(), config.getActivityId(), config.getDequeueBatchSize(), config.getMode().name(), config.isResetDaily()));

                    entity.setDequeueBatchSize(config.getDequeueBatchSize());
                    entity.setMode(config.getMode().name());
                    entity.setResetDaily(config.isResetDaily());
                    return jpaRepository.save(entity);
                })
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(savedEntity -> {
                    // 2. Sync to Redis Cache
                    try {
                        QueueConfigRedisEntity redisEntity = new QueueConfigRedisEntity(config.getTenantId(), config.getActivityId(), config.getDequeueBatchSize(), config.getMode().name(), config.isResetDaily());
                        String json = objectMapper.writeValueAsString(redisEntity);
                        return redisTemplate.opsForValue().set(getRedisKey(config.getTenantId(), config.getActivityId()), json)
                                .thenReturn(config);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("Failed to serialize QueueConfig to JSON", e));
                    }
                }).toFuture();
    }

    @Override
    public CompletableFuture<QueueConfig> findByTenantIdAndActivityId(String tenantId, String activityId) {
        String redisKey = getRedisKey(tenantId, activityId);

        // 1. Try to read from Redis
        return redisTemplate.opsForValue().get(redisKey)
                .flatMap(json -> {
                    try {
                        QueueConfigRedisEntity redisEntity = objectMapper.readValue(json, QueueConfigRedisEntity.class);
                        QueueMode mode = redisEntity.getMode() != null ?
                                QueueMode.valueOf(redisEntity.getMode()) :
                                QueueMode.AUTOMATIC;
                        QueueConfig config = QueueConfig.reconstitute(redisEntity.getTenantId(), redisEntity.getActivityId(),
                                redisEntity.getDequeueBatchSize(), mode, redisEntity.getResetDaily() != null ? redisEntity.getResetDaily() : true);
                        return Mono.just(config);
                    } catch (JsonProcessingException e) {
                        return Mono.error(new RuntimeException("Failed to deserialize QueueConfig from JSON", e));
                    }
                })
                // 2. If empty (cache miss), fallback to JPA
                .switchIfEmpty(
                        Mono.fromCallable(() -> jpaRepository.findByTenantIdAndActivityId(tenantId, activityId).orElse(null))
                                .subscribeOn(Schedulers.boundedElastic())
                                .flatMap(entity -> {
                                    if (entity != null) {
                                        QueueMode mode = entity.getMode() != null ?
                                                QueueMode.valueOf(entity.getMode()) :
                                                QueueMode.AUTOMATIC;
                                        QueueConfig config = QueueConfig.reconstitute(entity.getTenantId(), entity.getActivityId(), entity.getDequeueBatchSize(), mode, entity.getResetDaily() != null ? entity.getResetDaily() : true);
                                        // 3. Write back to Redis
                                        try {
                                            QueueConfigRedisEntity redisEntity = new QueueConfigRedisEntity(config.getTenantId(), config.getActivityId(), config.getDequeueBatchSize(), config.getMode().name(), config.isResetDaily());
                                            String json = objectMapper.writeValueAsString(redisEntity);
                                            return redisTemplate.opsForValue().set(redisKey, json).thenReturn(config);
                                        } catch (JsonProcessingException e) {
                                            return Mono.error(new RuntimeException("Failed to serialize QueueConfig to JSON", e));
                                        }
                                    }
                                    return Mono.empty(); // Not found in both Redis and JPA
                                })
                ).toFuture();
    }
}
