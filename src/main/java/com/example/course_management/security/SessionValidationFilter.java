package com.example.course_management.security;

import com.example.course_management.repository.UserRepository;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

public class SessionValidationFilter extends OncePerRequestFilter {
  private final UserRepository users;

  public SessionValidationFilter(UserRepository users) {
    this.users = users;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request, HttpServletResponse response, FilterChain chain)
      throws ServletException, IOException {
    var auth = SecurityContextHolder.getContext().getAuthentication();
    if (auth != null && auth.getPrincipal() instanceof CustomUserDetails principal) {
      var current = users.findById(principal.getUser().getUserId()).orElse(null);
      if (current == null
          || !Boolean.TRUE.equals(current.getIsActive())
          || current.getAuthVersion() != principal.getUser().getAuthVersion()) {
        var session = request.getSession(false);
        if (session != null) session.invalidate();
        SecurityContextHolder.clearContext();
      } else {
        var refreshed = new CustomUserDetails(current);
        SecurityContextHolder.getContext()
            .setAuthentication(
                new UsernamePasswordAuthenticationToken(
                    refreshed, null, refreshed.getAuthorities()));
      }
    }
    chain.doFilter(request, response);
  }
}
