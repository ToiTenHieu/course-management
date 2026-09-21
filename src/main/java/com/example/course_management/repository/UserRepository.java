package com.example.course_management.repository;

import com.example.course_management.entity.Role;
import com.example.course_management.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    @Query("SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) AND (:isActive IS NULL OR u.isActive = :isActive)")
    List<User> search(@Param("role") Role role, @Param("isActive") Boolean isActive);
}
