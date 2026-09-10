package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.subscriptionmanager.backend.dto.dashboard.CategorySpendResponse;
import com.subscriptionmanager.backend.entity.Subscription;
import com.subscriptionmanager.backend.entity.SubscriptionCategory;
import com.subscriptionmanager.backend.entity.enums.BillingCycle;

class CategoryBreakdownServiceTest {

    private final CategoryBreakdownService service = new CategoryBreakdownService(new CostNormalizationService());

    private SubscriptionCategory category(long id, String name) {
        SubscriptionCategory category = new SubscriptionCategory();
        category.setId(id);
        category.setName(name);
        category.setIcon("icon");
        category.setColor("#000000");
        return category;
    }

    private Subscription subscription(BigDecimal price, BillingCycle cycle, SubscriptionCategory category) {
        Subscription subscription = new Subscription();
        subscription.setPrice(price);
        subscription.setBillingCycle(cycle);
        subscription.setCategory(category);
        return subscription;
    }

    @Test
    void groupsSubscriptionsByCategoryAndSumsMonthlyCost() {
        SubscriptionCategory streaming = category(1L, "Streaming");
        List<Subscription> subs = List.of(
            subscription(new BigDecimal("10.00"), BillingCycle.MONTHLY, streaming),
            subscription(new BigDecimal("5.00"), BillingCycle.MONTHLY, streaming)
        );

        List<CategorySpendResponse> result = service.build(subs, new BigDecimal("15.00"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryId()).isEqualTo(1L);
        assertThat(result.get(0).monthlyAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    void uncategorizedSubscriptionsAreGroupedTogetherUnderNullCategory() {
        List<Subscription> subs = List.of(
            subscription(new BigDecimal("10.00"), BillingCycle.MONTHLY, null),
            subscription(new BigDecimal("5.00"), BillingCycle.MONTHLY, null)
        );

        List<CategorySpendResponse> result = service.build(subs, new BigDecimal("15.00"));

        assertThat(result).hasSize(1);
        assertThat(result.get(0).categoryId()).isNull();
        assertThat(result.get(0).categoryName()).isEqualTo("Uncategorized");
        assertThat(result.get(0).monthlyAmount()).isEqualByComparingTo("15.00");
    }

    @Test
    void percentageIsShareOfTotalMonthlySpend() {
        SubscriptionCategory streaming = category(1L, "Streaming");
        SubscriptionCategory music = category(2L, "Music");
        List<Subscription> subs = List.of(
            subscription(new BigDecimal("30.00"), BillingCycle.MONTHLY, streaming),
            subscription(new BigDecimal("10.00"), BillingCycle.MONTHLY, music)
        );

        List<CategorySpendResponse> result = service.build(subs, new BigDecimal("40.00"));

        CategorySpendResponse streamingResult = result.stream()
            .filter(r -> r.categoryId().equals(1L)).findFirst().orElseThrow();
        CategorySpendResponse musicResult = result.stream()
            .filter(r -> r.categoryId().equals(2L)).findFirst().orElseThrow();

        assertThat(streamingResult.percentage()).isEqualByComparingTo("75.0");
        assertThat(musicResult.percentage()).isEqualByComparingTo("25.0");
    }

    @Test
    void percentageIsZeroWhenTotalMonthlySpendIsZero() {
        List<Subscription> subs = List.of(subscription(BigDecimal.ZERO, BillingCycle.MONTHLY, null));

        List<CategorySpendResponse> result = service.build(subs, BigDecimal.ZERO);

        assertThat(result.get(0).percentage()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void resultsAreSortedByMonthlyAmountDescending() {
        SubscriptionCategory small = category(1L, "Small");
        SubscriptionCategory big = category(2L, "Big");
        List<Subscription> subs = List.of(
            subscription(new BigDecimal("5.00"), BillingCycle.MONTHLY, small),
            subscription(new BigDecimal("50.00"), BillingCycle.MONTHLY, big)
        );

        List<CategorySpendResponse> result = service.build(subs, new BigDecimal("55.00"));

        assertThat(result).extracting(CategorySpendResponse::categoryName).containsExactly("Big", "Small");
    }

    @Test
    void emptySubscriptionListProducesEmptyResult() {
        assertThat(service.build(List.of(), BigDecimal.ZERO)).isEmpty();
    }
}
