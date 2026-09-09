package com.subscriptionmanager.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.subscriptionmanager.backend.entity.SubscriptionCategory;

public interface SubscriptionCategoryRepository extends JpaRepository<SubscriptionCategory, Long> {

    @Query("""
        select c from SubscriptionCategory c
        where c.user is null or c.user.id = :userId
        order by c.name
        """)
    List<SubscriptionCategory> findDefaultsAndOwnedBy(@Param("userId") Long userId);

    List<SubscriptionCategory> findByUserIsNull();
}
