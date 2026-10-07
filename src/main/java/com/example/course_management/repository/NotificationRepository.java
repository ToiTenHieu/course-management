package com.example.course_management.repository;

import com.example.course_management.entity.Notification;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Integer> {
  List<Notification> findByUser_UserIdOrderByCreatedAtDesc(Integer userId);
}
