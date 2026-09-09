package com.subscriptionmanager.backend.config;

import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import com.subscriptionmanager.backend.entity.SubscriptionCategory;
import com.subscriptionmanager.backend.repository.SubscriptionCategoryRepository;

import lombok.RequiredArgsConstructor;

/**
 * Seeds the shared, system-wide subscription categories (rows with no
 * owning user) on first startup. Idempotent: skipped once any default
 * category already exists.
 */
@Component
@RequiredArgsConstructor
public class DefaultCategorySeeder implements CommandLineRunner {

    private record DefaultCategory(String name, String icon, String color) {
    }

    private static final List<DefaultCategory> DEFAULTS = List.of(
        new DefaultCategory("Streaming", "🎬", "#E50914"),
        new DefaultCategory("Music", "🎵", "#1DB954"),
        new DefaultCategory("Gaming", "🎮", "#6441A5"),
        new DefaultCategory("Software & SaaS", "💻", "#4285F4"),
        new DefaultCategory("Cloud Storage", "☁️", "#0061FF"),
        new DefaultCategory("News & Magazines", "📰", "#FF6600"),
        new DefaultCategory("Fitness & Health", "💪", "#00C853"),
        new DefaultCategory("Education", "📚", "#FF9800"),
        new DefaultCategory("Utilities", "🔧", "#607D8B"),
        new DefaultCategory("Other", "📦", "#9E9E9E")
    );

    private final SubscriptionCategoryRepository categoryRepository;

    @Override
    public void run(String... args) {
        if (!categoryRepository.findByUserIsNull().isEmpty()) {
            return;
        }

        List<SubscriptionCategory> categories = DEFAULTS.stream().map(d -> {
            SubscriptionCategory category = new SubscriptionCategory();
            category.setName(d.name());
            category.setIcon(d.icon());
            category.setColor(d.color());
            return category;
        }).toList();

        categoryRepository.saveAll(categories);
    }
}
