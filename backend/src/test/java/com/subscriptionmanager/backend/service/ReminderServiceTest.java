package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.subscriptionmanager.backend.entity.Budget;
import com.subscriptionmanager.backend.entity.Notification;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.User;
import com.subscriptionmanager.backend.entity.UserPreferences;
import com.subscriptionmanager.backend.entity.enums.BillingCycle;
import com.subscriptionmanager.backend.entity.enums.NotificationType;
import com.subscriptionmanager.backend.repository.BudgetRepository;
import com.subscriptionmanager.backend.repository.NotificationRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;
import com.subscriptionmanager.backend.repository.UserPreferencesRepository;

@ExtendWith(MockitoExtension.class)
class ReminderServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private NotificationRepository notificationRepository;
    @Mock
    private UserPreferencesRepository userPreferencesRepository;
    @Mock
    private BudgetRepository budgetRepository;
    @Mock
    private BudgetService budgetService;

    private ReminderService service;

    @BeforeEach
    void setUp() {
        service = new ReminderService(
            subscriptionRepository, notificationRepository, userPreferencesRepository, budgetRepository, budgetService);
        ReflectionTestUtils.setField(service, "renewalReminderDays", 7);
        ReflectionTestUtils.setField(service, "trialReminderDays", 3);

        // generateReminders() always scans all three reminder types; default
        // every source to empty so each test only needs to stub what it cares about.
        when(subscriptionRepository.findActiveRenewingBetween(any(), any(), any())).thenReturn(List.of());
        when(subscriptionRepository.findActiveTrialsEndingBetween(any(), any(), any())).thenReturn(List.of());
        when(budgetRepository.findByPeriodMonth(any())).thenReturn(List.of());
    }

    private User user(long id) {
        User user = new User();
        user.setId(id);
        return user;
    }

    private Subscription subscription(long id, User owner, LocalDate nextBillingDate, LocalDate trialEndDate) {
        Subscription subscription = new Subscription();
        subscription.setId(id);
        subscription.setUser(owner);
        subscription.setName("Netflix");
        subscription.setPrice(new BigDecimal("9.99"));
        subscription.setCurrency("USD");
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        subscription.setNextBillingDate(nextBillingDate);
        subscription.setTrialEndDate(trialEndDate);
        return subscription;
    }

    @Test
    void createsRenewalReminderWhenNoneExistsYet() {
        User owner = user(1L);
        LocalDate nextBillingDate = LocalDate.now().plusDays(3);
        when(subscriptionRepository.findActiveRenewingBetween(any(), any(), any()))
            .thenReturn(List.of(subscription(1L, owner, nextBillingDate, null)));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndReferenceDate(
            1L, 1L, NotificationType.RENEWAL_UPCOMING, nextBillingDate)).thenReturn(false);

        service.generateReminders();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.RENEWAL_UPCOMING);
        assertThat(captor.getValue().getReferenceDate()).isEqualTo(nextBillingDate);
    }

    @Test
    void skipsRenewalReminderWhenAlreadyNotifiedForThatDate() {
        User owner = user(1L);
        LocalDate nextBillingDate = LocalDate.now().plusDays(3);
        when(subscriptionRepository.findActiveRenewingBetween(any(), any(), any()))
            .thenReturn(List.of(subscription(1L, owner, nextBillingDate, null)));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndReferenceDate(
            1L, 1L, NotificationType.RENEWAL_UPCOMING, nextBillingDate)).thenReturn(true);

        service.generateReminders();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void skipsRenewalReminderWhenUserHasDisabledNotifications() {
        User owner = user(1L);
        LocalDate nextBillingDate = LocalDate.now().plusDays(3);
        when(subscriptionRepository.findActiveRenewingBetween(any(), any(), any()))
            .thenReturn(List.of(subscription(1L, owner, nextBillingDate, null)));
        UserPreferences disabled = new UserPreferences();
        disabled.setNotificationsEnabled(false);
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(disabled));

        service.generateReminders();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createsTrialEndingReminderWhenNoneExistsYet() {
        User owner = user(1L);
        LocalDate trialEndDate = LocalDate.now().plusDays(2);
        when(subscriptionRepository.findActiveTrialsEndingBetween(any(), any(), any()))
            .thenReturn(List.of(subscription(1L, owner, null, trialEndDate)));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(notificationRepository.existsByUserIdAndSubscriptionIdAndTypeAndReferenceDate(
            1L, 1L, NotificationType.TRIAL_ENDING, trialEndDate)).thenReturn(false);

        service.generateReminders();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.TRIAL_ENDING);
    }

    @Test
    void createsBudgetExceededAlertWhenProjectedSpendPassesBudget() {
        LocalDate periodMonth = LocalDate.now().withDayOfMonth(1);
        Budget budget = new Budget();
        budget.setUser(user(1L));
        budget.setAmount(new BigDecimal("100.00"));
        budget.setPeriodMonth(periodMonth);
        when(budgetRepository.findByPeriodMonth(any())).thenReturn(List.of(budget));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(budgetService.projectedMonthlySpend(1L)).thenReturn(new BigDecimal("150.00"));
        when(notificationRepository.existsByUserIdAndTypeAndReferenceDate(1L, NotificationType.BUDGET_EXCEEDED, periodMonth))
            .thenReturn(false);

        service.generateReminders();

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.BUDGET_EXCEEDED);
    }

    @Test
    void skipsBudgetAlertWhenProjectedSpendIsWithinBudget() {
        Budget budget = new Budget();
        budget.setUser(user(1L));
        budget.setAmount(new BigDecimal("100.00"));
        budget.setPeriodMonth(LocalDate.now().withDayOfMonth(1));
        when(budgetRepository.findByPeriodMonth(any())).thenReturn(List.of(budget));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(budgetService.projectedMonthlySpend(1L)).thenReturn(new BigDecimal("50.00"));

        service.generateReminders();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void skipsBudgetAlertWhenAlreadyNotifiedThisPeriod() {
        LocalDate periodMonth = LocalDate.now().withDayOfMonth(1);
        Budget budget = new Budget();
        budget.setUser(user(1L));
        budget.setAmount(new BigDecimal("100.00"));
        budget.setPeriodMonth(periodMonth);
        when(budgetRepository.findByPeriodMonth(any())).thenReturn(List.of(budget));
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.empty());
        when(budgetService.projectedMonthlySpend(1L)).thenReturn(new BigDecimal("150.00"));
        when(notificationRepository.existsByUserIdAndTypeAndReferenceDate(1L, NotificationType.BUDGET_EXCEEDED, periodMonth))
            .thenReturn(true);

        service.generateReminders();

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void skipsBudgetAlertWhenUserHasDisabledNotifications() {
        Budget budget = new Budget();
        budget.setUser(user(1L));
        budget.setAmount(new BigDecimal("100.00"));
        budget.setPeriodMonth(LocalDate.now().withDayOfMonth(1));
        when(budgetRepository.findByPeriodMonth(any())).thenReturn(List.of(budget));
        UserPreferences disabled = new UserPreferences();
        disabled.setNotificationsEnabled(false);
        when(userPreferencesRepository.findByUserId(1L)).thenReturn(Optional.of(disabled));

        service.generateReminders();

        verify(notificationRepository, never()).save(any());
        verify(budgetService, never()).projectedMonthlySpend(anyLong());
    }
}
