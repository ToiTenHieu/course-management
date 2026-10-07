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

  public DemoScenarioRunner(DemoScenarioSeeder seeder) {
    this.seeder = seeder;
  }

  @Override
  public void run(String... args) {
    seeder.seed();
  }
}
