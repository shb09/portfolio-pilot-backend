package com.portfoliopilot.repository;

import com.portfoliopilot.entity.AnalyticsEvent;
import com.portfoliopilot.entity.EventType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalyticsEventRepository extends JpaRepository<AnalyticsEvent, Long> {

    long countByOwnerUserIdAndEventType(Long ownerUserId, EventType eventType);

    long countByOwnerUserId(Long ownerUserId);
}
