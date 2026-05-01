package com.chenhaonee.agents.domain.notify.repository;

import com.chenhaonee.agents.domain.notify.model.Notification;
import com.chenhaonee.agents.domain.notify.model.Notification.NotificationStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * 通知消息仓储。
 */
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    Optional<Notification> findByCode(String code);

    Page<Notification> findByValidTrueOrderByCreateTimeDesc(Pageable pageable);

    long countByValidTrueAndStatusAndReadAtIsNull(NotificationStatus status);

    /**
     * 查询所有待发送的通知，按创建时间升序。
     */
    List<Notification> findByStatusAndValidTrueOrderByCreateTimeAsc(NotificationStatus status);
}
