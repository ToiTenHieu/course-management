package com.example.course_management.config;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ApplicationReadiness {
  private volatile boolean ready;

  @EventListener(ApplicationReadyEvent.class)
  public void markReady() {
    ready = true;
  }

  public boolean isReady() {
    return ready;
  }
}
