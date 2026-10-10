package com.example.course_management.service;

import com.example.course_management.time.ApplicationTime;
import com.example.course_management.config.PasswordRecoverySettings;
import com.example.course_management.entity.Notification;
import com.example.course_management.exception.*;
import com.example.course_management.repository.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class PasswordRecoveryService {
  public static final String ACCEPTED="Nếu email thuộc tài khoản đang hoạt động, bạn sẽ nhận được liên kết đặt lại mật khẩu. Vui lòng kiểm tra thư đến và thư rác.";
  public static final String INVALID="Liên kết không hợp lệ hoặc đã hết hạn. Hãy yêu cầu một liên kết mới.";
  private record Delivery(String tokenHash,String email,String url,LocalDateTime expiresAt) {}
  private record Token(int userId,String email,long version,LocalDateTime expiresAt) {}
  private record Limit(Instant until,int count) {}
  private final JdbcTemplate jdbc;
  private final UserRepository users;
  private final NotificationRepository notifications;
  private final PasswordEncoder encoder;
  private final PasswordRecoverySettings settings;
  private final PasswordRecoveryDelivery delivery;
  private final ThreadPoolTaskExecutor executor;
  private final TransactionTemplate transaction;
  private final SecureRandom random=new SecureRandom();
  private final Map<String,Limit> limits=new HashMap<>();
  private final AtomicInteger outstandingJobs=new AtomicInteger();
  public PasswordRecoveryService(JdbcTemplate jdbc,UserRepository users,NotificationRepository notifications,
      PasswordEncoder encoder,PasswordRecoverySettings settings,PasswordRecoveryDelivery delivery,
      @Qualifier("passwordRecoveryExecutor") ThreadPoolTaskExecutor executor,PlatformTransactionManager manager) {
    this.jdbc=jdbc;this.users=users;this.notifications=notifications;this.encoder=encoder;this.settings=settings;this.delivery=delivery;
    this.executor=executor;this.transaction=new TransactionTemplate(manager);
  }
  public void request(String email,String remoteAddress) {
    requireEnabled();limit("request:"+remoteAddress,10);
    // All valid addresses take the same queueing path; account lookup/delivery happen after the response.
    outstandingJobs.incrementAndGet();
    try {executor.execute(()->{
      try {issue(email.trim().toLowerCase(Locale.ROOT));}
      catch(Exception failure) {org.slf4j.LoggerFactory.getLogger(getClass()).warn("Password recovery job failed ({})",failure.getClass().getSimpleName());}
      finally {outstandingJobs.decrementAndGet();}
    });}catch(RejectedExecutionException failure) {outstandingJobs.decrementAndGet();throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,"Dịch vụ đang bận. Vui lòng thử lại sau.");}
  }
  private void issue(String email) {
    Delivery mail=transaction.execute(tx->{
      var ids=jdbc.queryForList("SELECT user_id FROM users WHERE LOWER(email)=? AND is_active=TRUE",Integer.class,email);
      if(ids.size()!=1)return null;
      var account=users.findLockedById(ids.getFirst()).orElse(null);
      if(account==null||!Boolean.TRUE.equals(account.getIsActive())||!account.getEmail().equalsIgnoreCase(email))return null;
      var now=ApplicationTime.now();
      jdbc.update("DELETE FROM password_reset_tokens WHERE expires_at<=?",now);
      var last=jdbc.query("SELECT created_at FROM password_reset_tokens WHERE user_id=? ORDER BY created_at DESC LIMIT 1",(r,n)->r.getTimestamp(1).toLocalDateTime(),account.getUserId());
      if(!last.isEmpty()&&last.getFirst().isAfter(now.minusSeconds(60)))return null;
      byte[] bytes=new byte[32];random.nextBytes(bytes);String token=Base64.getUrlEncoder().withoutPadding().encodeToString(bytes),hash=hash(token);var expires=now.plusMinutes(15);
      jdbc.update("INSERT INTO password_reset_tokens(token_hash,user_id,email_snapshot,auth_version,created_at,expires_at) VALUES(?,?,?,?,?,?)",hash,account.getUserId(),account.getEmail(),account.getAuthVersion(),now,expires);
      return new Delivery(hash,account.getEmail(),settings.resetUrl(token),expires);
    });
    if(mail!=null) {
      try{delivery.send(mail.email(),mail.url(),mail.expiresAt());}
      catch(RuntimeException failure) {
        transaction.executeWithoutResult(tx->jdbc.update("DELETE FROM password_reset_tokens WHERE token_hash=?",mail.tokenHash()));
        throw failure;
      }
    }
  }
  public void reset(String token,String password,String remoteAddress) {
    requireEnabled();limit("reset:"+remoteAddress,30);
    if(token==null||!token.matches("[A-Za-z0-9_-]{43}"))throw new BadRequestException(INVALID);
    com.example.course_management.security.PasswordPolicy.validateNewPassword(password);
    String tokenHash=hash(token);
    transaction.executeWithoutResult(tx->{
      var rows=tokens(tokenHash);if(rows.isEmpty())throw new BadRequestException(INVALID);
      var account=users.findLockedById(rows.getFirst().userId()).orElseThrow(()->new BadRequestException(INVALID));
      // Re-read after the account lock: a concurrent reset can consume all tokens while we wait.
      rows=tokens(tokenHash);var now=ApplicationTime.now();
      if(rows.isEmpty()||!Boolean.TRUE.equals(account.getIsActive())||rows.getFirst().version()!=account.getAuthVersion()
          ||!rows.getFirst().email().equals(account.getEmail())||!rows.getFirst().expiresAt().isAfter(now))throw new BadRequestException(INVALID);
      account.setPasswordHash(encoder.encode(password));account.setAuthVersion(account.getAuthVersion()+1);account.setUpdatedAt(now);users.saveAndFlush(account);
      jdbc.update("DELETE FROM password_reset_tokens WHERE user_id=?",account.getUserId());
      var notice=new Notification();notice.setUser(account);notice.setType("PASSWORD_CHANGED");notice.setMessage("Mật khẩu đã được đặt lại qua liên kết khôi phục. Các phiên đăng nhập cũ đã hết hiệu lực.");notice.setTargetUrl("/profile.html");notifications.save(notice);
    });
  }
  private List<Token> tokens(String hash) {
    return jdbc.query("SELECT user_id,email_snapshot,auth_version,expires_at FROM password_reset_tokens WHERE token_hash=?",
        (r,n)->new Token(r.getInt(1),r.getString(2),r.getLong(3),r.getTimestamp(4).toLocalDateTime()),hash);
  }
  private void requireEnabled() {if(!settings.enabled())throw new ResourceNotFoundException("Lấy lại mật khẩu chưa được bật. Vui lòng liên hệ quản trị viên.");}
  private synchronized void limit(String key,int maximum) {
    var now=Instant.now();limits.entrySet().removeIf(entry->!entry.getValue().until().isAfter(now));
    var existing=limits.get(key);
    if(existing==null&&limits.size()>=10000||existing!=null&&existing.count()>=maximum)
      throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,"Bạn đã gửi nhiều yêu cầu. Vui lòng thử lại sau 15 phút.");
    limits.put(key,new Limit(existing==null?now.plusSeconds(900):existing.until(),existing==null?1:existing.count()+1));
  }
  public static String hash(String token) {
    try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));}
    catch(NoSuchAlgorithmException failure){throw new IllegalStateException(failure);}
  }
}
