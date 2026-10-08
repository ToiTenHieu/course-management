package com.example.course_management.repository;

import com.example.course_management.entity.Role;
import com.example.course_management.entity.User;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository
    extends JpaRepository<User, Integer>,
        org.springframework.data.jpa.repository.JpaSpecificationExecutor<User> {
  Optional<User> findByUsername(String username);

  boolean existsByUsername(String username);

  boolean existsByEmail(String email);

  @Query(
      "SELECT u FROM User u WHERE (:role IS NULL OR u.role = :role) AND (:isActive IS NULL OR"
          + " u.isActive = :isActive) ORDER BY u.fullName ASC")
  List<User> search(@Param("role") Role role, @Param("isActive") Boolean isActive);

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("SELECT x FROM User x WHERE x.userId = :id")
  Optional<User> findLockedById(@org.springframework.data.repository.query.Param("id") Integer id);
}
