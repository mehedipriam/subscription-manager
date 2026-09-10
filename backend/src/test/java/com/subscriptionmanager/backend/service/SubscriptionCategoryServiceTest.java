package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subscriptionmanager.backend.dto.subscription.CategoryRequest;
import com.subscriptionmanager.backend.dto.subscription.CategoryResponse;
import com.subscriptionmanager.backend.entity.SubscriptionCategory;
import com.subscriptionmanager.backend.entity.User;
import com.subscriptionmanager.backend.exception.CategoryInUseException;
import com.subscriptionmanager.backend.exception.ResourceNotFoundException;
import com.subscriptionmanager.backend.repository.SubscriptionCategoryRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;
import com.subscriptionmanager.backend.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class SubscriptionCategoryServiceTest {

    @Mock
    private SubscriptionCategoryRepository categoryRepository;
    @Mock
    private SubscriptionRepository subscriptionRepository;
    @Mock
    private UserRepository userRepository;

    private SubscriptionCategoryService service;

    @BeforeEach
    void setUp() {
        service = new SubscriptionCategoryService(categoryRepository, subscriptionRepository, userRepository);
    }

    @Test
    void listForUserReturnsDefaultsAndOwnCategories() {
        SubscriptionCategory category = new SubscriptionCategory();
        category.setId(1L);
        category.setName("Streaming");
        when(categoryRepository.findDefaultsAndOwnedBy(1L)).thenReturn(List.of(category));

        List<CategoryResponse> result = service.listForUser(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("Streaming");
    }

    @Test
    void createSavesACategoryOwnedByTheUser() {
        when(userRepository.getReferenceById(1L)).thenReturn(new User());
        when(categoryRepository.save(any(SubscriptionCategory.class))).thenAnswer(invocation -> {
            SubscriptionCategory saved = invocation.getArgument(0);
            saved.setId(7L);
            return saved;
        });

        CategoryResponse response = service.create(1L, new CategoryRequest("Custom", "🎮", "#123456"));

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.name()).isEqualTo("Custom");
        assertThat(response.isDefault()).isFalse();
    }

    @Test
    void deleteRemovesAnUnusedOwnedCategory() {
        SubscriptionCategory category = new SubscriptionCategory();
        category.setId(3L);
        when(categoryRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(category));
        when(subscriptionRepository.existsByCategoryIdAndDeletedAtIsNull(3L)).thenReturn(false);

        service.delete(1L, 3L);

        verify(subscriptionRepository).clearCategoryReferences(3L);
        verify(categoryRepository).delete(category);
    }

    @Test
    void deleteThrowsWhenCategoryNotOwnedByUser() {
        when(categoryRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.delete(1L, 3L)).isInstanceOf(ResourceNotFoundException.class);
        verify(categoryRepository, never()).delete(any());
    }

    @Test
    void deleteThrowsWhenCategoryIsStillInUse() {
        SubscriptionCategory category = new SubscriptionCategory();
        category.setId(3L);
        when(categoryRepository.findByIdAndUserId(3L, 1L)).thenReturn(Optional.of(category));
        when(subscriptionRepository.existsByCategoryIdAndDeletedAtIsNull(3L)).thenReturn(true);

        assertThatThrownBy(() -> service.delete(1L, 3L)).isInstanceOf(CategoryInUseException.class);
        verify(categoryRepository, never()).delete(any());
        verify(subscriptionRepository, never()).clearCategoryReferences(any());
    }
}
