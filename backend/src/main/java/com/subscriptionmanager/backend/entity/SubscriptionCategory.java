package com.subscriptionmanager.backend.entity;

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
 * A category a subscription can belong to. Rows with a null {@code user}
 * are system-provided defaults shared by everyone; rows with a user are
 * that user's own custom categories.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(
    name = "subscription_categories",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "name"})
)
public class SubscriptionCategory extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 50)
    private String icon;

    @Column(length = 20)
    private String color;
}
