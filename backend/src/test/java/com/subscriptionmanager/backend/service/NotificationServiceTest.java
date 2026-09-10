package com.subscriptionmanager.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.subscriptionmanager.backend.dto.notification.NotificationResponse;
import com.subscriptionmanager.backend.entity.Notification;
import com.subscriptionmanager.backend.entity.enums.NotificationType;
import com.subscriptionmanager.backend.exception.ResourceNotFoundException;
import com.subscriptionmanager.backend.repository.NotificationRepository;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    private NotificationService service;

    @BeforeEach
    void setUp() {
        service = new NotificationService(notificationRepository);
    }

    private Notification notification(long id, boolean isRead) {
        Notification notification = new Notification();
        notification.setId(id);
        notification.setType(NotificationType.RENEWAL_UPCOMING);
        notification.setMessage("test");
        notification.setRead(isRead);
        return notification;
    }

    @Test
    void listForUserReturnsAllWhenUnreadOnlyIsFalse() {
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L))
            .thenReturn(List.of(notification(1L, true), notification(2L, false)));

        List<NotificationResponse> result = service.listForUser(1L, false);

        assertThat(result).hasSize(2);
    }

    @Test
    void listForUserReturnsOnlyUnreadWhenUnreadOnlyIsTrue() {
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L))
            .thenReturn(List.of(notification(2L, false)));

        List<NotificationResponse> result = service.listForUser(1L, true);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isRead()).isFalse();
    }

    @Test
    void unreadCountDelegatesToRepository() {
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(3L);

        assertThat(service.unreadCount(1L)).isEqualTo(3L);
    }

    @Test
    void markReadFlipsTheReadFlagForAnOwnedNotification() {
        Notification notification = notification(2L, false);
        when(notificationRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(notification)).thenReturn(notification);

        NotificationResponse response = service.markRead(1L, 2L);

        assertThat(response.isRead()).isTrue();
    }

    @Test
    void markReadThrowsWhenNotificationNotOwnedByUser() {
        when(notificationRepository.findByIdAndUserId(2L, 1L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.markRead(1L, 2L)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void markAllReadFlipsEveryUnreadNotificationAndSavesThemAll() {
        Notification first = notification(1L, false);
        Notification second = notification(2L, false);
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L))
            .thenReturn(List.of(first, second));

        service.markAllRead(1L);

        assertThat(first.isRead()).isTrue();
        assertThat(second.isRead()).isTrue();
        verify(notificationRepository, times(1)).saveAll(anyList());
        verify(notificationRepository, never()).save(any());
    }
}
