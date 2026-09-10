package com.subscriptionmanager.backend.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.subscriptionmanager.backend.entity.Budget;

public interface BudgetRepository extends JpaRepository<Budget, Long> {

    Optional<Budget> findByUserIdAndPeriodMonth(Long userId, LocalDate periodMonth);

    List<Budget> findByUserIdOrderByPeriodMonthDesc(Long userId);

    List<Budget> findByPeriodMonth(LocalDate periodMonth);
}
