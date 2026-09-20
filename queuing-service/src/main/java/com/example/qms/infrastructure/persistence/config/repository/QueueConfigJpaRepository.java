package com.example.qms.infrastructure.persistence.config.repository;

import com.example.qms.infrastructure.persistence.config.entity.QueueConfigJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface QueueConfigJpaRepository extends JpaRepository<QueueConfigJpaEntity, Long> {
    Optional<QueueConfigJpaEntity> findByTenantIdAndActivityId(String tenantId, String activityId);
}
