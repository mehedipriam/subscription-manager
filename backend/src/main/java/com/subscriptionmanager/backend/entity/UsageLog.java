package com.subscriptionmanager.backend.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A single "I used this" check-in against a subscription, logged manually
 * by the user. Used to flag subscriptions that haven't been touched in a
 * while as candidates for cancellation.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "usage_logs")
public class UsageLog extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @Column(name = "used_at", nullable = false)
    private Instant usedAt;
}
