package com.subscriptionmanager.backend.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * A user's spending budget for a given month, stored as the first day of
 * that month (e.g. 2026-09-01) so each user has at most one budget per month.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "budgets",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "period_month"})
)
public class Budget extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "period_month", nullable = false)
    private LocalDate periodMonth;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal amount;
}
