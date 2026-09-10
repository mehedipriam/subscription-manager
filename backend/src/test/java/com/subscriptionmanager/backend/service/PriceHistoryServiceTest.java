package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subscriptionmanager.backend.dto.subscription.PriceHistoryResponse;
import com.subscriptionmanager.backend.entity.PriceHistory;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.exception.ResourceNotFoundException;
import com.subscriptionmanager.backend.repository.PriceHistoryRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;

@ExtendWith(MockitoExtension.class)
class PriceHistoryServiceTest {

    @Mock
    private PriceHistoryRepository priceHistoryRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;

    private PriceHistoryService service;

    @BeforeEach
    void setUp() {
        service = new PriceHistoryService(priceHistoryRepository, subscriptionRepository);
    }

    @Test
    void listForSubscriptionReturnsHistoryWithComputedPercentageChange() {
        Subscription subscription = new Subscription();
        subscription.setId(5L);
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(5L, 1L)).thenReturn(Optional.of(subscription));

        PriceHistory change = new PriceHistory();
        change.setId(1L);
        change.setOldPrice(new BigDecimal("10.00"));
        change.setNewPrice(new BigDecimal("12.00"));
        change.setChangedAt(Instant.now());
        when(priceHistoryRepository.findBySubscriptionIdOrderByChangedAtDesc(5L)).thenReturn(List.of(change));

        List<PriceHistoryResponse> result = service.listForSubscription(1L, 5L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).percentageChange()).isEqualByComparingTo("20.00");
    }

    @Test
    void listForSubscriptionThrowsWhenSubscriptionNotOwnedByUser() {
        when(subscriptionRepository.findByIdAndUserIdAndDeletedAtIsNull(5L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.listForSubscription(1L, 5L)).isInstanceOf(ResourceNotFoundException.class);
    }
}
