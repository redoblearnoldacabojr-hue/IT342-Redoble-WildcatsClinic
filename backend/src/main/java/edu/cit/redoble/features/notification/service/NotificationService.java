package edu.cit.redoble.features.notification.service;

import edu.cit.redoble.features.notification.entity.NotificationEntity;
import edu.cit.redoble.features.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    public NotificationEntity create(Long userId, String message) {
        NotificationEntity notification = new NotificationEntity();
        notification.setUserId(userId);
        notification.setMessage(message);
        notification.setRead(false);
        return notificationRepository.save(notification);
    }

    public List<NotificationEntity> getForUser(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public int markAllRead(Long userId) {
        List<NotificationEntity> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(userId);
        int updated = 0;

        for (NotificationEntity notification : notifications) {
            if (!notification.isRead()) {
                notification.setRead(true);
                updated += 1;
            }
        }

        if (updated > 0) {
            notificationRepository.saveAll(notifications);
        }

        return updated;
    }
}