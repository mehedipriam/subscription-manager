package com.subscriptionmanager.backend.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.subscriptionmanager.backend.dto.subscription.CategoryRequest;
import com.subscriptionmanager.backend.dto.subscription.CategoryResponse;
import com.subscriptionmanager.backend.entity.SubscriptionCategory;
import com.subscriptionmanager.backend.entity.User;
import com.subscriptionmanager.backend.exception.CategoryInUseException;
import com.subscriptionmanager.backend.exception.ResourceNotFoundException;
import com.subscriptionmanager.backend.repository.SubscriptionCategoryRepository;
import com.subscriptionmanager.backend.repository.SubscriptionRepository;
import com.subscriptionmanager.backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SubscriptionCategoryService {

    private final SubscriptionCategoryRepository categoryRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final UserRepository userRepository;

    public List<CategoryResponse> listForUser(Long userId) {
        return categoryRepository.findDefaultsAndOwnedBy(userId).stream()
            .map(CategoryResponse::from)
            .toList();
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        User user = userRepository.getReferenceById(userId);

        SubscriptionCategory category = new SubscriptionCategory();
        category.setUser(user);
        category.setName(request.name());
        category.setIcon(request.icon());
        category.setColor(request.color());

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Transactional
    public void delete(Long userId, Long categoryId) {
        SubscriptionCategory category = categoryRepository.findByIdAndUserId(categoryId, userId)
            .orElseThrow(() -> new ResourceNotFoundException("Category not found"));

        if (subscriptionRepository.existsByCategoryIdAndDeletedAtIsNull(categoryId)) {
            throw new CategoryInUseException();
        }

        subscriptionRepository.clearCategoryReferences(categoryId);
        categoryRepository.delete(category);
    }
}
