package com.example.course_management.repository;

import com.example.course_management.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository
    extends JpaRepository<Notification, Integer>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<Notification> {
  @org.springframework.data.jpa.repository.Modifying
  @org.springframework.data.jpa.repository.Query(
      "UPDATE Notification n SET n.isRead = true WHERE n.user.userId = :id AND n.isRead = false")
  int markAllRead(@org.springframework.data.repository.query.Param("id") Integer id);

  List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);
}
