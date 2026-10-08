package com.example.course_management.config;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(2)
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DemoScenarioRunner implements CommandLineRunner {
  private final DemoScenarioSeeder seeder;
  private final DemoYouTubeSeeder youtube;

  public DemoScenarioRunner(DemoScenarioSeeder seeder, DemoYouTubeSeeder youtube) {
    this.seeder = seeder;
    this.youtube = youtube;
  }

  @Override
  public void run(String... args) {
    seeder.seed();
    youtube.seed();
  }
}
