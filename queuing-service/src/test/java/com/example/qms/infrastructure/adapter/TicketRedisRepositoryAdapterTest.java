package com.example.qms.infrastructure.adapter;

import com.example.qms.domain.ticket.aggregate.vo.ActivityId;
import com.example.qms.domain.ticket.aggregate.vo.TenantId;
import com.example.qms.domain.ticket.aggregate.root.Ticket;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.data.redis.core.ReactiveValueOperations;
import org.springframework.data.redis.core.script.RedisScript;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

class TicketRedisRepositoryAdapterTest {

    /**
     * Feature: Ticket Enqueue Operations
     * 
     * Scenario: Enqueueing a ticket executes the Lua script and returns a valid Ticket aggregate
     *   Given a valid TenantId "tenant-x" and ActivityId "act-y"
     *   And the Redis server successfully executes the enqueue Lua script returning ticket number 50
     *   When the TicketRedisAdapter enqueues a new ticket
     *   Then a Ticket aggregate is returned with ticket number 50 and status QUEUING
     */
    @Test
    @SuppressWarnings("unchecked")
    void testEnqueueExecutesLuaScript() {
        // Arrange
        ReactiveStringRedisTemplate redisTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
        RedisTicketRepositoryAdapter adapter = new RedisTicketRepositoryAdapter(redisTemplate);

        TenantId tenantId = new TenantId("tenant-x");
        ActivityId activityId = new ActivityId("act-y");

        when(redisTemplate.execute(any(RedisScript.class), any(List.class), any(List.class)))
                .thenReturn(Flux.just(50L));

        // Act
        Ticket ticket = adapter.enqueue(tenantId, activityId, "20260920").join();

        // Assert
        assert ticket.getTicketNumber() == 50L;
        assert ticket.getTenantId().equals(tenantId);
        assert ticket.getStatus().name().equals("QUEUING");
    }

    /**
     * Feature: Get Current Serving Number
     * 
     * Scenario: Retrieving the current serving number returns the latest processed ticket number
     *   Given a valid TenantId "tenant-x" and ActivityId "act-y"
     *   And the Redis server currently has the serving number "123" for this activity
     *   When the TicketRedisAdapter requests the current serving number
     *   Then the adapter should return the number 123
     */
    @Test
    void testGetCurrentServingNumber() {
        // Arrange
        ReactiveStringRedisTemplate redisTemplate = Mockito.mock(ReactiveStringRedisTemplate.class);
        ReactiveValueOperations<String, String> valOps = Mockito.mock(ReactiveValueOperations.class);
        
        when(redisTemplate.opsForValue()).thenReturn(valOps);
        when(valOps.get("qms:tenant:tenant-x:activity:act-y:serving:20260920")).thenReturn(Mono.just("123"));

        RedisTicketRepositoryAdapter adapter = new RedisTicketRepositoryAdapter(redisTemplate);

        // Act
        Long servingNumber = adapter.getCurrentServingNumber(new TenantId("tenant-x"), new ActivityId("act-y"), "20260920").join();

        // Assert
        assert servingNumber == 123L;
    }
}
