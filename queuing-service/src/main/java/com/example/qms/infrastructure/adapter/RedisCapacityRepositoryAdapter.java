package com.example.qms.infrastructure.adapter;

import com.example.qms.application.port.out.CapacityRepositoryPort;
import com.example.qms.domain.ticket.aggregate.vo.ActivityId;
import com.example.qms.domain.ticket.aggregate.vo.TenantId;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Repository;

import com.example.qms.domain.ticket.aggregate.vo.CapacityState;

import java.util.List;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

@Repository
class RedisCapacityRepositoryAdapter implements CapacityRepositoryPort {

    private final ReactiveStringRedisTemplate redisTemplate;

    // Lua script to atomically check capacity and deduct up to requestedAmount
    private static final String TRY_ACQUIRE_LUA_SCRIPT =
            "local cap = tonumber(redis.call('GET', KEYS[1]) or '0'); " +
            "if cap <= 0 then return 0; end; " +
            "local take = math.min(cap, tonumber(ARGV[1])); " +
            "redis.call('DECRBY', KEYS[1], take); " +
            "return take;";

    public RedisCapacityRepositoryAdapter(ReactiveStringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    private String getCapacityKey(TenantId tenantId, ActivityId activityId) {
        // Use hash tag {} just in case we need to operate with other keys in a script in the future
        return String.format("qms:{tenant:%s:activity:%s}:capacity", tenantId.value(), activityId.value());
    }

    private String getMaxCapacityKey(TenantId tenantId, ActivityId activityId) {
        return String.format("qms:{tenant:%s:activity:%s}:max_capacity", tenantId.value(), activityId.value());
    }

    @Override
    public CompletableFuture<Integer> tryAcquireCapacity(TenantId tenantId, ActivityId activityId, int requestedAmount) {
        if (requestedAmount <= 0) {
            return CompletableFuture.completedFuture(0);
        }

        String key = getCapacityKey(tenantId, activityId);

        return redisTemplate.execute(
                RedisScript.of(TRY_ACQUIRE_LUA_SCRIPT, Long.class),
                List.of(key),
                List.of(String.valueOf(requestedAmount))
        ).next().map(Long::intValue).toFuture();
    }

    @Override
    public CompletableFuture<Void> releaseCapacity(TenantId tenantId, ActivityId activityId, int releasedAmount) {
        if (releasedAmount <= 0) {
            return CompletableFuture.completedFuture(null);
        }

        String key = getCapacityKey(tenantId, activityId);

        return redisTemplate.opsForValue().increment(key, releasedAmount)
                .then().toFuture();
    }

    @Override
    public CompletableFuture<CapacityState> getCapacity(TenantId tenantId, ActivityId activityId) {
        String capKey = getCapacityKey(tenantId, activityId);
        String maxKey = getMaxCapacityKey(tenantId, activityId);

        return redisTemplate.opsForValue().multiGet(List.of(capKey, maxKey))
                .map(values -> {
                    int remaining = values.get(0) != null ? Integer.parseInt(values.get(0)) : 0;
                    int max = values.get(1) != null ? Integer.parseInt(values.get(1)) : 0;
                    return new CapacityState(remaining, max);
                })
                .toFuture();
    }

    private static final String SET_MAX_CAPACITY_LUA_SCRIPT =
            "local capKey = KEYS[1]; " +
            "local maxKey = KEYS[2]; " +
            "local newMax = tonumber(ARGV[1]); " +
            "local oldMax = tonumber(redis.call('GET', maxKey) or '0'); " +
            "local oldCap = tonumber(redis.call('GET', capKey) or '0'); " +
            "local diff = newMax - oldMax; " +
            "redis.call('SET', maxKey, newMax); " +
            "redis.call('INCRBY', capKey, diff); " +
            "return 1;";

    @Override
    public CompletableFuture<Void> setCapacity(TenantId tenantId, ActivityId activityId, int maxCapacity) {
        String capKey = getCapacityKey(tenantId, activityId);
        String maxKey = getMaxCapacityKey(tenantId, activityId);

        return redisTemplate.execute(
                RedisScript.of(SET_MAX_CAPACITY_LUA_SCRIPT, Long.class),
                List.of(capKey, maxKey),
                List.of(String.valueOf(maxCapacity))
        ).next().then().toFuture();
    }

    private final String REMOVE_AND_RELEASE_LUA_SCRIPT = 
            "local activeSessionsKey = KEYS[1]; " +
            "local capacityKey = KEYS[2]; " +
            "local maxKey = KEYS[3]; " +
            "local ticketNumber = ARGV[1]; " +
            "local releaseAmount = tonumber(ARGV[2]); " +
            "local removed = redis.call('ZREM', activeSessionsKey, ticketNumber); " +
            "if removed == 1 then " +
            "    local currentCap = tonumber(redis.call('GET', capacityKey) or '0'); " +
            "    local maxCap = tonumber(redis.call('GET', maxKey) or '0'); " +
            "    if currentCap + releaseAmount <= maxCap then " +
            "        redis.call('INCRBY', capacityKey, releaseAmount); " +
            "    else " +
            "        redis.call('SET', capacityKey, maxCap); " +
            "    end; " +
            "    return 1; " +
            "else " +
            "    return 0; " +
            "end;";

    private String getActiveSessionsKey(TenantId tenantId, ActivityId activityId, String dateSuffix) {
        String base = String.format("qms:{tenant:%s:activity:%s}:active_sessions", tenantId.value(), activityId.value());
        return dateSuffix != null && !dateSuffix.isBlank() ? base + ":" + dateSuffix : base;
    }

    @Override
    public CompletableFuture<Boolean> removeActiveSessionAndRelease(TenantId tenantId, ActivityId activityId, long ticketNumber, int releasedAmount, String dateSuffix) {
        String activeSessionsKey = getActiveSessionsKey(tenantId, activityId, dateSuffix);
        String capacityKey = getCapacityKey(tenantId, activityId);
        String maxKey = getMaxCapacityKey(tenantId, activityId);

        return redisTemplate.execute(
                RedisScript.of(REMOVE_AND_RELEASE_LUA_SCRIPT, Long.class),
                List.of(activeSessionsKey, capacityKey, maxKey),
                List.of(String.valueOf(ticketNumber), String.valueOf(releasedAmount))
        ).next()
         .map(result -> result == 1L)
         .toFuture();
    }
}
