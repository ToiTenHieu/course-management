package com.example.course_management.config;

import com.example.course_management.repository.CourseRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(0)
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoData implements CommandLineRunner {
  private static final String KEY = "base-demo-courses-v1";
  private final DemoCatalog catalog;
  private final DemoAccountSeeder accounts;
  private final DemoCourseSeeder seeder;
  private final CourseRepository courses;
  private final JdbcTemplate jdbc;

  public DemoData(DemoCatalog catalog, DemoAccountSeeder accounts, DemoCourseSeeder seeder,
      CourseRepository courses, JdbcTemplate jdbc) {
    this.catalog = catalog; this.accounts = accounts; this.seeder = seeder; this.courses = courses; this.jdbc = jdbc;
  }

  @Override
  @Transactional
  public void run(String... args) {
    accounts.initializeLoginAccounts();
    if (jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs WHERE dataset_key = ?", Integer.class, KEY) > 0) return;
    // Existing demo databases keep their current courses, including instructor edits.
    if (courses.count() == 0) catalog.data().baseCourses().forEach(seeder::create);
    jdbc.update("INSERT INTO demo_seed_runs (dataset_key, completed_at) VALUES (?, ?)", KEY, com.example.course_management.time.ApplicationTime.now());
  }
}
