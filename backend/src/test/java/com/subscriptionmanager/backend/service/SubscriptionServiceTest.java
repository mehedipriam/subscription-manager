package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
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

import com.subscriptionmanager.backend.dto.subscription.SubscriptionRequest;
import com.subscriptionmanager.backend.dto.subscription.SubscriptionResponse;
import com.subscriptionmanager.backend.entity.PriceHistory;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.SubscriptionCategory;
import com.subscriptionmanager.backend.entity.User;
import com.subscriptionmanager.backend.entity.enums.BillingCycle;
import com.subscriptionmanager.backend.exception.ResourceNotFoundException;
import com.subscriptionmanager.backend.repository.PriceHistoryRepository;
import com.subscriptionmanager.backend.repository.SubscriptionCategoryRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;
import com.subscriptionmanager.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceTest {

    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private SubscriptionCategoryRepository categoryRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PriceHistoryRepository priceHistoryRepository;

    private SubscriptionService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionService(subscriptionRepository, categoryRepository, userRepository, priceHistoryRepository);
        lenient().when(subscriptionRepository.save(any(Subscription.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private SubscriptionRequest basicRequest(BigDecimal price, String currency) {
        return new SubscriptionRequest(
            "Netflix", null, null, price, currency, BillingCycle.MONTHLY,
            LocalDate.of(2020, 1, 1), LocalDate.of(2026, 12, 1), null, null, null, null, null, null);
    }

    @Test
    void listForUserReturnsOnlyThatUsersNonDeletedSubscriptions() {
        Subscription subscription = new Subscription();
        subscription.setId(1L);
        subscription.setName("Netflix");
        subscription.setPrice(BigDecimal.TEN);
        subscription.setBillingCycle(BillingCycle.MONTHLY);
        when(subscriptionRepository.findByUserIdAndDeletedAtIsNull(1L)).thenReturn(List.of(subscription));

        List<SubscriptionResponse> result = service.listForUser(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Netflix");
    }

    @Test
    void getForUserThrowsWhenSubscriptionNotOwned() {
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(1L, 10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getForUser(10L, 1L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createDefaultsCurrencyToUsdWhenBlank() {
        when(userRepository.getReferenceById(1L)).thenReturn(new User());

        SubscriptionResponse response = service.create(1L, basicRequest(new BigDecimal("9.99"), " "));

        assertThat(response.currency()).isEqualTo("USD");
    }

    @Test
    void createUppercasesProvidedCurrency() {
        when(userRepository.getReferenceById(1L)).thenReturn(new User());

        SubscriptionResponse response = service.create(1L, basicRequest(new BigDecimal("9.99"), "eur"));

        assertThat(response.currency()).isEqualTo("EUR");
    }

    @Test
    void createLeavesCategoryNullWhenNoCategoryIdProvided() {
        when(userRepository.getReferenceById(1L)).thenReturn(new User());

        SubscriptionResponse response = service.create(1L, basicRequest(new BigDecimal("9.99"), "USD"));

        assertThat(response.category()).isNull();
    }

    @Test
    void createAcceptsADefaultCategoryOwnedByNoOne() {
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        SubscriptionCategory defaultCategory = new SubscriptionCategory();
        defaultCategory.setId(2L);
        defaultCategory.setName("Streaming");
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(defaultCategory));

        SubscriptionRequest request = new SubscriptionRequest(
            "Netflix", null, 2L, new BigDecimal("9.99"), "USD", BillingCycle.MONTHLY,
            LocalDate.of(2020, 1, 1), LocalDate.of(2026, 12, 1), null, null, null, null, null, null);
        SubscriptionResponse response = service.create(1L, request);

        assertThat(response.category().id()).isEqualTo(2L);
    }

    @Test
    void createThrowsWhenCategoryBelongsToAnotherUser() {
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        SubscriptionCategory othersCategory = new SubscriptionCategory();
        othersCategory.setId(2L);
        User otherUser = new User();
        otherUser.setId(99L);
        othersCategory.setUser(otherUser);
        when(categoryRepository.findById(2L)).thenReturn(Optional.of(othersCategory));

        SubscriptionRequest request = new SubscriptionRequest(
            "Netflix", null, 2L, new BigDecimal("9.99"), "USD", BillingCycle.MONTHLY,
            LocalDate.of(2020, 1, 1), LocalDate.of(2026, 12, 1), null, null, null, null, null, null);

        assertThatThrownBy(() -> service.create(1L, request)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateRecordsPriceHistoryWhenPriceChanges() {
        Subscription existing = new Subscription();
        existing.setId(1L);
        existing.setPrice(new BigDecimal("9.99"));
        existing.setBillingCycle(BillingCycle.MONTHLY);
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(1L, 10L)).thenReturn(Optional.of(existing));

        service.update(10L, 1L, basicRequest(new BigDecimal("14.99"), "USD"));

        verify(priceHistoryRepository).save(any(PriceHistory.class));
    }

    @Test
    void updateDoesNotRecordPriceHistoryWhenPriceIsUnchanged() {
        Subscription existing = new Subscription();
        existing.setId(1L);
        existing.setPrice(new BigDecimal("9.99"));
        existing.setBillingCycle(BillingCycle.MONTHLY);
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(1L, 10L)).thenReturn(Optional.of(existing));

        service.update(10L, 1L, basicRequest(new BigDecimal("9.99"), "USD"));

        verify(priceHistoryRepository, never()).save(any());
    }

    @Test
    void deleteSoftDeletesBySettingDeletedAt() {
        Subscription existing = new Subscription();
        existing.setId(1L);
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(1L, 10L)).thenReturn(Optional.of(existing));

        service.delete(10L, 1L);

        assertThat(existing.getDeletedAt()).isNotNull();
        verify(subscriptionRepository).save(existing);
    }

    @Test
    void createComputesNextBillingDateByAdvancingPastTodayWhenNotProvided() {
        when(userRepository.getReferenceById(1L)).thenReturn(new User());

        SubscriptionRequest request = new SubscriptionRequest(
            "Netflix", null, null, new BigDecimal("9.99"), "USD", BillingCycle.MONTHLY,
            LocalDate.now().minusMonths(2), null, null, null, null, null, null, null);
        SubscriptionResponse response = service.create(1L, request);

        assertThat(response.nextBillingDate()).isAfterOrEqualTo(LocalDate.now());
    }

    @Test
    void createUsesExplicitNextBillingDateWhenProvided() {
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        LocalDate explicit = LocalDate.of(2027, 6, 1);

        SubscriptionRequest request = new SubscriptionRequest(
            "Netflix", null, null, new BigDecimal("9.99"), "USD", BillingCycle.MONTHLY,
            LocalDate.of(2020, 1, 1), explicit, null, null, null, null, null, null);
        SubscriptionResponse response = service.create(1L, request);

        assertThat(response.nextBillingDate()).isEqualTo(explicit);
    }
}
