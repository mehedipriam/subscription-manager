package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subscriptionmanager.backend.dto.budget.BudgetRequest;
import com.subscriptionmanager.backend.dto.budget.BudgetResponse;
import com.subscriptionmanager.backend.dto.budget.BudgetStatusResponse;
import com.subscriptionmanager.backend.entity.Budget;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.User;
import com.subscriptionmanager.backend.entity.enums.BillingCycle;
import com.subscriptionmanager.backend.entity.enums.SubscriptionStatus;
import com.subscriptionmanager.backend.repository.BudgetRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;
import com.subscriptionmanager.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class BudgetServiceTest {

    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;

    private BudgetService service;

    @BeforeEach
    void setUp() {
        service = new BudgetService(budgetRepository, subscriptionRepository, userRepository, new CostNormalizationService());
    }

    private Subscription subscription(BigDecimal price, BillingCycle cycle, SubscriptionStatus status) {
        Subscription subscription = new Subscription();
        subscription.setPrice(price);
        subscription.setBillingCycle(cycle);
        subscription.setStatus(status);
        return subscription;
    }

    @Test
    void projectedMonthlySpendOnlyCountsActiveSubscriptions() {
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of(
            subscription(new BigDecimal("10.00"), BillingCycle.MONTHLY, SubscriptionStatus.ACTIVE),
            subscription(new BigDecimal("100.00"), BillingCycle.MONTHLY, SubscriptionStatus.CANCELLED)
        ));

        assertThat(service.projectedMonthlySpend(1L)).isEqualByComparingTo("10.00");
    }

    @Test
    void getStatusReturnsNullBudgetFieldsWhenNoBudgetSet() {
        when(budgetRepository.findByUserIdAndPeriodMonth(eq(1L), any())).thenReturn(Optional.empty());

        BudgetStatusResponse status = service.getStatus(1L, LocalDate.of(2026, 3, 15), new BigDecimal("50.00"));

        assertThat(status.budgetAmount()).isNull();
        assertThat(status.remaining()).isNull();
        assertThat(status.percentageUsed()).isNull();
        assertThat(status.exceeded()).isFalse();
        assertThat(status.periodMonth()).isEqualTo(LocalDate.of(2026, 3, 1));
    }

    @Test
    void getStatusReportsExceededWhenProjectedSpendPassesBudget() {
        Budget budget = new Budget();
        budget.setAmount(new BigDecimal("100.00"));
        when(budgetRepository.findByUserIdAndPeriodMonth(eq(1L), any())).thenReturn(Optional.of(budget));

        BudgetStatusResponse status = service.getStatus(1L, LocalDate.of(2026, 3, 15), new BigDecimal("150.00"));

        assertThat(status.exceeded()).isTrue();
        assertThat(status.remaining()).isEqualByComparingTo("-50.00");
    }

    @Test
    void historyReturnsBudgetsNewestPeriodFirst() {
        Budget older = new Budget();
        older.setPeriodMonth(LocalDate.of(2026, 1, 1));
        older.setAmount(new BigDecimal("50.00"));
        Budget newer = new Budget();
        newer.setPeriodMonth(LocalDate.of(2026, 2, 1));
        newer.setAmount(new BigDecimal("60.00"));
        when(budgetRepository.findByUserIdOrderByPeriodMonthDesc(1L)).thenReturn(List.of(newer, older));

        List<BudgetResponse> history = service.history(1L);

        assertThat(history).extracting(BudgetResponse::periodMonth)
            .containsExactly(LocalDate.of(2026, 2, 1), LocalDate.of(2026, 1, 1));
    }

    @Test
    void upsertCreatesNewBudgetWhenNoneExistsForThePeriod() {
        when(budgetRepository.findByUserIdAndPeriodMonth(eq(1L), any())).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetRequest request = new BudgetRequest(new BigDecimal("200.00"), LocalDate.of(2026, 5, 10));
        BudgetResponse response = service.upsert(1L, request);

        assertThat(response.amount()).isEqualByComparingTo("200.00");
        assertThat(response.periodMonth()).isEqualTo(LocalDate.of(2026, 5, 1));
    }

    @Test
    void upsertUpdatesExistingBudgetInsteadOfCreatingANewOne() {
        Budget existing = new Budget();
        existing.setId(9L);
        existing.setPeriodMonth(LocalDate.of(2026, 5, 1));
        existing.setAmount(new BigDecimal("100.00"));
        when(budgetRepository.findByUserIdAndPeriodMonth(eq(1L), any())).thenReturn(Optional.of(existing));
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetRequest request = new BudgetRequest(new BigDecimal("250.00"), LocalDate.of(2026, 5, 10));
        BudgetResponse response = service.upsert(1L, request);

        assertThat(response.id()).isEqualTo(9L);
        assertThat(response.amount()).isEqualByComparingTo("250.00");
        verify(userRepository, never()).getReferenceById(any());
    }

    @Test
    void upsertDefaultsPeriodMonthToCurrentMonthWhenNotProvided() {
        when(budgetRepository.findByUserIdAndPeriodMonth(eq(1L), any())).thenReturn(Optional.empty());
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(budgetRepository.save(any(Budget.class))).thenAnswer(invocation -> invocation.getArgument(0));

        BudgetRequest request = new BudgetRequest(new BigDecimal("75.00"), null);
        BudgetResponse response = service.upsert(1L, request);

        assertThat(response.periodMonth()).isEqualTo(LocalDate.now().withDayOfMonth(1));
    }
}
