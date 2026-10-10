package com.example.course_management.config;

import com.example.course_management.time.ApplicationTime;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TimeConfig {
  @Bean
  public Clock applicationClock() {
    return Clock.system(ApplicationTime.ZONE);
  }
}
