package com.example.course_management.config;

import com.example.course_management.repository.UserRepository;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class DemoYouTubeSeeder {
  private static final String KEY = "youtube-demo-courses-v1";
  private final JdbcTemplate jdbc;
  private final DemoCatalog catalog;
  private final DemoCourseSeeder courses;
  private final UserRepository users;

  public DemoYouTubeSeeder(JdbcTemplate jdbc, DemoCatalog catalog, DemoCourseSeeder courses, UserRepository users) {
    this.jdbc = jdbc; this.catalog = catalog; this.courses = courses; this.users = users;
  }

  @Transactional
  public void seed() {
    if (jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs WHERE dataset_key = ?", Integer.class, KEY) > 0) return;
    if (users.findByUsername(catalog.account("teacher").username()).isEmpty()) return;
    catalog.data().youtubeCourses().forEach(courses::create);
    jdbc.update("INSERT INTO demo_seed_runs (dataset_key) VALUES (?)", KEY);
  }
}
