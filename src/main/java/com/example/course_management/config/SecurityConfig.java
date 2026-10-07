package com.example.course_management.config;

import com.example.course_management.repository.UserRepository;
import com.example.course_management.security.*;
import org.springframework.context.annotation.*;
import org.springframework.security.authentication.*;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.*;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.csrf.CsrfTokenRequestAttributeHandler;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  @Bean
  public AuthenticationManager authenticationManager(
      CustomUserDetailsService users, PasswordEncoder encoder) {
    var provider = new DaoAuthenticationProvider(users);
    provider.setPasswordEncoder(encoder);
    return new ProviderManager(provider);
  }

  @Bean
  public SecurityFilterChain filterChain(HttpSecurity http, UserRepository users) throws Exception {
    http.csrf(csrf -> csrf.csrfTokenRequestHandler(new CsrfTokenRequestAttributeHandler()))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        "/",
                        "/*.html",
                        "/css/**",
                        "/js/**",
                        "/assets/**",
                        "/favicon.ico",
                        "/api/auth/login",
                        "/api/auth/register",
                        "/api/auth/csrf",
                        "/api/auth/session",
                        "/api/auth/config")
                    .permitAll()
                    .requestMatchers(org.springframework.http.HttpMethod.GET, "/api/discovery/**")
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            errors ->
                errors
                    .authenticationEntryPoint(
                        (req, res, ex) -> {
                          res.setStatus(401);
                          res.setContentType("application/json;charset=UTF-8");
                          res.getWriter()
                              .write(
                                  "{\"success\":false,\"message\":\"Vui lòng đăng"
                                      + " nhập\",\"data\":null}");
                        })
                    .accessDeniedHandler(
                        (req, res, ex) -> {
                          res.setStatus(403);
                          res.setContentType("application/json;charset=UTF-8");
                          res.getWriter()
                              .write(
                                  "{\"success\":false,\"message\":\"Không có quyền hoặc phiên bảo"
                                      + " vệ đã hết hạn\",\"data\":null}");
                        }))
        .addFilterAfter(new SessionValidationFilter(users), SecurityContextHolderFilter.class);
    return http.build();
  }
}
