package com.example.course_management.config;

import com.example.course_management.entity.Role;
import com.example.course_management.repository.CourseRepository;
import com.example.course_management.repository.UserRepository;
import java.util.stream.Stream;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Add the new public metadata once to existing demo records without replacing user edits. */
@Component
public class DemoProfileSeeder {
  private static final String KEY = "demo-public-profiles-and-course-fit-v1";
  private final DemoCatalog catalog;
  private final UserRepository users;
  private final CourseRepository courses;
  private final JdbcTemplate jdbc;

  public DemoProfileSeeder(DemoCatalog catalog, UserRepository users, CourseRepository courses, JdbcTemplate jdbc) {
    this.catalog = catalog; this.users = users; this.courses = courses; this.jdbc = jdbc;
  }

  @Transactional
  public void seed() {
    if (jdbc.queryForObject("SELECT COUNT(*) FROM demo_seed_runs WHERE dataset_key = ?", Integer.class, KEY) > 0) return;
    var data = catalog.data();
    var fallback = data.courseFitDefaults().stream().filter(f -> f.category().isEmpty()).findFirst()
        .orElseThrow(() -> new IllegalStateException("Missing default demo course preparation"));
    var samples = Stream.of(data.baseCourses(), data.youtubeCourses(), data.scenarioCourses())
        .flatMap(java.util.Collection::stream).toList();
    boolean foundTeacher = false;
    for (var spec : data.accounts()) {
      if (spec.role() != Role.TEACHER) continue;
      var existing = users.findByUsername(spec.username()).orElse(null);
      if (existing == null) continue;
      var teacher = users.findLockedById(existing.getUserId()).orElseThrow();
      if (teacher.getRole() != Role.TEACHER) continue;
      foundTeacher = true;
      if (blank(teacher.getBiography())) teacher.setBiography(spec.biography());
      if (blank(teacher.getExpertise())) teacher.setExpertise(spec.expertise());
      for (var existingCourse : courses.findByTeacher_UserId(teacher.getUserId())) {
        var course = courses.findLockedById(existingCourse.getCourseId()).orElseThrow();
        var sample = samples.stream().filter(s -> s.teacher().equals(spec.key()) && s.title().equals(course.getTitle()))
            .findFirst().orElse(null);
        var defaults = data.courseFitDefaults().stream().filter(f -> f.category().equals(course.getCategory()))
            .findFirst().orElse(fallback);
        if (blank(course.getTargetAudience()))
          course.setTargetAudience(sample == null ? defaults.targetAudience() : sample.targetAudience());
        if (blank(course.getPrerequisites()))
          course.setPrerequisites(sample == null ? defaults.prerequisites() : sample.prerequisites());
      }
    }
    // Missing demo accounts must not cause this enrichment to create accounts or consume its marker.
    if (foundTeacher) jdbc.update("INSERT INTO demo_seed_runs (dataset_key, completed_at) VALUES (?, ?)", KEY, com.example.course_management.time.ApplicationTime.now());
  }

  private boolean blank(String value) {
    return value == null || value.isBlank();
  }
}
