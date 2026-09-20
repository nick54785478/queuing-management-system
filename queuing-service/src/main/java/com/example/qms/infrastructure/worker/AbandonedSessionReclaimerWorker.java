package com.example.qms.infrastructure.worker;

import com.example.qms.application.port.out.QueueConfigRepositoryPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;

import java.util.List;

@Component
public class AbandonedSessionReclaimerWorker {

    private static final Logger log = LoggerFactory.getLogger(AbandonedSessionReclaimerWorker.class);

    private final ReactiveStringRedisTemplate redisTemplate;
    private final QueueConfigRepositoryPort queueConfigRepositoryPort;

    public AbandonedSessionReclaimerWorker(ReactiveStringRedisTemplate redisTemplate, QueueConfigRepositoryPort queueConfigRepositoryPort) {
        this.redisTemplate = redisTemplate;
        this.queueConfigRepositoryPort = queueConfigRepositoryPort;
    }

    private final String RECLAIM_LUA_SCRIPT = 
            "local activeSessionsKey = KEYS[1]; " +
            "local capacityKey = KEYS[2]; " +
            "local maxKey = KEYS[3]; " +
            "local currentTimestamp = tonumber(ARGV[1]); " +
            // Timeout is 30 seconds
            "local cutoffTimestamp = currentTimestamp - 30000; " +
            
            // Get all expired tickets
            "local expiredTickets = redis.call('ZRANGEBYSCORE', activeSessionsKey, '-inf', cutoffTimestamp); " +
            "if #expiredTickets > 0 then " +
            "    local count = #expiredTickets; " +
            "    redis.call('ZREMRANGEBYSCORE', activeSessionsKey, '-inf', cutoffTimestamp); " +
            "    local currentCap = tonumber(redis.call('GET', capacityKey) or '0'); " +
            "    local maxCap = tonumber(redis.call('GET', maxKey) or '0'); " +
            "    if currentCap + count <= maxCap then " +
            "        redis.call('INCRBY', capacityKey, count); " +
            "    else " +
            "        redis.call('SET', capacityKey, maxCap); " +
            "    end; " +
            "    return count; " +
            "else " +
            "    return 0; " +
            "end;";

    @Scheduled(fixedDelay = 10000)
    public void reclaimAbandonedSessions() {
        String tenantId = "demo-tenant";
        String activityId = "demo-activity";
        
        queueConfigRepositoryPort.findByTenantIdAndActivityId(tenantId, activityId)
            .thenAccept(config -> {
                String dateSuffix = config != null && config.isResetDaily() ? java.time.LocalDate.now().toString() : "";

                String activeSessionsKey = getActiveSessionsKey(tenantId, activityId, dateSuffix);
                String capacityKey = getCapacityKey(tenantId, activityId);
                String maxKey = getMaxCapacityKey(tenantId, activityId);

                long currentTimestamp = System.currentTimeMillis();

                redisTemplate.execute(
                        RedisScript.of(RECLAIM_LUA_SCRIPT, Long.class),
                        List.of(activeSessionsKey, capacityKey, maxKey),
                        List.of(String.valueOf(currentTimestamp))
                ).next()
                 .subscribe(count -> {
                     if (count != null && count > 0) {
                         log.info("Reclaimed {} abandoned sessions for tenant {}, activity {}", count, tenantId, activityId);
                     }
                 }, error -> log.error("Failed to reclaim abandoned sessions for tenant {}, activity {}", tenantId, activityId, error));
            });
    }

    private String getActiveSessionsKey(String tenantId, String activityId, String dateSuffix) {
        String base = String.format("qms:{tenant:%s:activity:%s}:active_sessions", tenantId, activityId);
        return dateSuffix != null && !dateSuffix.isBlank() ? base + ":" + dateSuffix : base;
    }

    private String getCapacityKey(String tenantId, String activityId) {
        return String.format("qms:{tenant:%s:activity:%s}:capacity", tenantId, activityId);
    }

    private String getMaxCapacityKey(String tenantId, String activityId) {
        return String.format("qms:{tenant:%s:activity:%s}:max_capacity", tenantId, activityId);
    }
}
