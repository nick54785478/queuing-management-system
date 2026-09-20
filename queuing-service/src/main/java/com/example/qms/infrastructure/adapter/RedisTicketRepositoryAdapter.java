package com.example.qms.infrastructure.adapter;

import com.example.qms.application.port.out.TicketRepositoryPort;
import com.example.qms.domain.ticket.aggregate.root.Ticket;
import com.example.qms.domain.ticket.aggregate.vo.ActivityId;
import com.example.qms.domain.ticket.aggregate.vo.TenantId;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.time.Duration;
import java.util.UUID;

@Repository
class RedisTicketRepositoryAdapter implements TicketRepositoryPort {

    private final ReactiveStringRedisTemplate redisTemplate;
    
    // Simple Lua script for INCR and ZADD (In a real scenario, this would check capacity)
    private final String LUA_SCRIPT = 
            "local ticketNum = redis.call('INCR', KEYS[1]); " +
            "redis.call('ZADD', KEYS[2], ticketNum, ARGV[1]); " +
            "return ticketNum;";

    RedisTicketRepositoryAdapter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getCounterKey(TenantId tenantId, ActivityId activityId, String dateSuffix) {
        String base = String.format("qms:{tenant:%s:activity:%s}:counter", tenantId.value(), activityId.value());
        return dateSuffix != null && !dateSuffix.isBlank() ? base + ":" + dateSuffix : base;
    }

    private String getQueueKey(TenantId tenantId, ActivityId activityId, String dateSuffix) {
        String base = String.format("qms:{tenant:%s:activity:%s}:queue", tenantId.value(), activityId.value());
        return dateSuffix != null && !dateSuffix.isBlank() ? base + ":" + dateSuffix : base;
    }

    private String getServingKey(TenantId tenantId, ActivityId activityId, String dateSuffix) {
        String base = String.format("qms:{tenant:%s:activity:%s}:serving", tenantId.value(), activityId.value());
        return dateSuffix != null && !dateSuffix.isBlank() ? base + ":" + dateSuffix : base;
    }

    private String getTokenKey(TenantId tenantId, ActivityId activityId, String queueToken, String dateSuffix) {
        String base = String.format("qms:{tenant:%s:activity:%s}:token:%s", tenantId.value(), activityId.value(), queueToken);
        return dateSuffix != null && !dateSuffix.isBlank() ? base + ":" + dateSuffix : base;
    }

    @Override
    public CompletableFuture<Ticket> enqueue(TenantId tenantId, ActivityId activityId, String dateSuffix) {
        String counterKey = getCounterKey(tenantId, activityId, dateSuffix);
        String queueKey = getQueueKey(tenantId, activityId, dateSuffix);
        
        String memberId = UUID.randomUUID().toString();

        return redisTemplate.execute(
                RedisScript.of(LUA_SCRIPT, Long.class),
                List.of(counterKey, queueKey),
                List.of(memberId)
        ).next().map(ticketNum -> new Ticket(tenantId, activityId, ticketNum)).toFuture();
    }

    @Override
    public CompletableFuture<Long> getCurrentServingNumber(TenantId tenantId, ActivityId activityId, String dateSuffix) {
        String servingKey = getServingKey(tenantId, activityId, dateSuffix);
        return redisTemplate.opsForValue().get(servingKey)
                .map(Long::parseLong)
                .defaultIfEmpty(0L)
                .toFuture();
    }

    private String getActiveSessionsKey(TenantId tenantId, ActivityId activityId, String dateSuffix) {
        String base = String.format("qms:{tenant:%s:activity:%s}:active_sessions", tenantId.value(), activityId.value());
        return dateSuffix != null && !dateSuffix.isBlank() ? base + ":" + dateSuffix : base;
    }

    @Override
    public CompletableFuture<Void> updateActiveSession(TenantId tenantId, ActivityId activityId, String queueToken, long ticketNumber, String dateSuffix) {
        String activeSessionsKey = getActiveSessionsKey(tenantId, activityId, dateSuffix);
        long currentTimestamp = System.currentTimeMillis();
        // ZADD with current timestamp. When dequeued, the worker will also ZADD with current timestamp.
        return redisTemplate.opsForZSet().add(activeSessionsKey, String.valueOf(ticketNumber), currentTimestamp).then().toFuture();
    }

    private final String DEQUEUE_LUA_SCRIPT = 
            "local popped = redis.call('ZPOPMIN', KEYS[1], tonumber(ARGV[1])); " +
            "if #popped > 0 then " +
            "    local maxScore = popped[#popped]; " +
            "    redis.call('SET', KEYS[2], maxScore); " +
            "    for i=1,#popped,2 do " +
            "        redis.call('ZADD', KEYS[3], ARGV[2], popped[i+1]); " +
            "    end; " +
            "end; " +
            "return popped;";

    @Override
    public Flow.Publisher<Ticket> dequeue(TenantId tenantId, ActivityId activityId, int limit, String dateSuffix) {
        String queueKey = getQueueKey(tenantId, activityId, dateSuffix);
        String servingKey = getServingKey(tenantId, activityId, dateSuffix);

        String activeSessionsKey = getActiveSessionsKey(tenantId, activityId, dateSuffix);
        long currentTimestamp = System.currentTimeMillis();

        Flux<Ticket> flux = redisTemplate.execute(
                RedisScript.of(DEQUEUE_LUA_SCRIPT, List.class),
                List.of(queueKey, servingKey, activeSessionsKey),
                List.of(String.valueOf(limit), String.valueOf(currentTimestamp))
        ).flatMapIterable(list -> (List<?>) list)
         .buffer(2)
         .map(tuple -> {
             String member = (String) tuple.get(0);
             Long score = Long.parseLong(String.valueOf(tuple.get(1)));
             return new Ticket(tenantId, activityId, score);
         });
         
        return reactor.adapter.JdkFlowAdapter.publisherToFlowPublisher(flux);
    }
}
