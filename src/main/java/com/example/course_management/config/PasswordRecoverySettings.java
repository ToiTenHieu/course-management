package com.example.course_management.config;

import java.net.URI;
import java.util.Set;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

@Component
public class PasswordRecoverySettings {
  private final String mode,baseUrl,from;
  public PasswordRecoverySettings(@Value("${app.recovery.mode:disabled}") String mode,
      @Value("${app.recovery.public-base-url:}") String baseUrl,
      @Value("${app.recovery.from-address:}") String from,
      @Value("${app.demo.enabled:false}") boolean demo,ObjectProvider<JavaMailSender> sender) {
    if(!Set.of("disabled","demo","smtp").contains(mode)) throw new IllegalArgumentException("PASSWORD_RESET_MODE phải là disabled, demo hoặc smtp");
    if(mode.equals("demo")&&!demo) throw new IllegalArgumentException("Hộp thư thử nghiệm chỉ được bật trong bản demo");
    if(!mode.equals("disabled")) {
      URI uri=URI.create(baseUrl);
      if(uri.getHost()==null||uri.getUserInfo()!=null||uri.getQuery()!=null||uri.getFragment()!=null
          ||!(uri.getPath().isEmpty()||uri.getPath().equals("/"))||!("https".equals(uri.getScheme())
          ||mode.equals("demo")&&"http".equals(uri.getScheme())&&Set.of("127.0.0.1","localhost","[::1]").contains(uri.getHost())))
        throw new IllegalArgumentException("PASSWORD_RESET_BASE_URL phải là địa chỉ gốc HTTPS; demo cho phép HTTP trên localhost");
      if(mode.equals("smtp")&&(sender.getIfAvailable()==null||!from.matches("[^\\s@<>]+@[^\\s@<>]+\\.[^\\s@<>]+")))
        throw new IllegalArgumentException("Chế độ SMTP cần spring.mail.host và PASSWORD_RESET_FROM hợp lệ");
    }
    this.mode=mode;this.baseUrl=baseUrl.replaceAll("/$","");this.from=from;
  }
  public boolean enabled() {return !mode.equals("disabled");}
  public boolean demoMailbox() {return mode.equals("demo");}
  public String resetUrl(String token) {return baseUrl+"/password-recovery.html#token="+token;}
  public String from() {return from;}
  @Bean("passwordRecoveryExecutor") public ThreadPoolTaskExecutor executor() {
    var executor=new ThreadPoolTaskExecutor();executor.setCorePoolSize(2);executor.setMaxPoolSize(2);executor.setQueueCapacity(100);
    executor.setThreadNamePrefix("password-recovery-");executor.setWaitForTasksToCompleteOnShutdown(true);executor.setAwaitTerminationSeconds(10);return executor;
  }
}
