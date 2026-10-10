package com.example.course_management.config;

import com.example.course_management.entity.*;
import com.example.course_management.repository.UserRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoAccountSeeder {
  private final DemoCatalog catalog;
  private final UserRepository users;
  private final PasswordEncoder encoder;
  private final String password;
  private final String emailDomain;
  private final org.springframework.jdbc.core.JdbcTemplate jdbc;

  public DemoAccountSeeder(DemoCatalog catalog, UserRepository users, PasswordEncoder encoder,
      @Value("${app.demo.password}") String password, @Value("${app.demo.email-domain}") String emailDomain,
      org.springframework.jdbc.core.JdbcTemplate jdbc) {
    this.catalog = catalog; this.users = users; this.encoder = encoder;
    this.password = password; this.emailDomain = emailDomain;
    this.jdbc = jdbc;
  }

  @Transactional
  public User account(String key) {
    var spec = catalog.account(key);
    return users.findByUsername(spec.username()).orElseGet(() -> {
      var user = account(spec.username(), spec.fullName(), spec.role());
      user.setBiography(spec.biography());
      user.setExpertise(spec.expertise());
      return users.saveAndFlush(user);
    });
  }

  @Transactional
  public User account(String username, String fullName, Role role) {
    return users.findByUsername(username).orElseGet(() -> {
      var user = new User();
      user.setUsername(username); user.setFullName(fullName); user.setRole(role);
      user.setEmail(username + "@" + emailDomain); user.setPasswordHash(encoder.encode(password));
      return users.saveAndFlush(user);
    });
  }

  @Transactional(readOnly = true)
  public List<LoginAccount> loginAccounts() {
    return jdbc.queryForList("SELECT user_id FROM demo_login_accounts ORDER BY user_id", Integer.class).stream()
        .flatMap(id -> users.findById(id).stream())
        .filter(user -> Boolean.TRUE.equals(user.getIsActive()) && encoder.matches(password, user.getPasswordHash()))
        .map(user -> new LoginAccount(user.getUsername(), user.getRole(), password)).toList();
  }

  @Transactional
  public void initializeLoginAccounts() {
    String key = "demo-login-accounts-v1";
    if (jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs WHERE dataset_key = ?", Integer.class, key) > 0) return;
    catalog.data().accounts().stream().filter(DemoCatalog.Account::quickLogin).forEach(spec -> {
      var user = account(spec.key());
      jdbc.update("INSERT INTO demo_login_accounts (user_id) VALUES (?)", user.getUserId());
    });
    jdbc.update("INSERT INTO demo_seed_runs (dataset_key) VALUES (?)", key);
  }

  public record LoginAccount(String username, Role role, String password) {}
}
