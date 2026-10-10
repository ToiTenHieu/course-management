package com.example.course_management;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.example.course_management.entity.*;
import com.example.course_management.exception.BadRequestException;
import com.example.course_management.repository.*;
import com.example.course_management.security.CustomUserDetails;
import com.example.course_management.service.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties={"app.recovery.mode=demo","app.demo.enabled=true","app.recovery.public-base-url=http://127.0.0.1:8080"})
@AutoConfigureMockMvc @ActiveProfiles("test")
class PasswordRecoveryTests {
  @Autowired MockMvc mvc;
  @Autowired UserRepository users;
  @Autowired PasswordEncoder encoder;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordRecoveryService recovery;
  @MockitoSpyBean PasswordRecoveryDelivery delivery;
  @MockitoSpyBean NotificationRepository notices;
  final List<Integer> owned=new ArrayList<>();
  User student,admin;
  String address;
  @BeforeEach void setup() {
    reset(delivery,notices);address=UUID.randomUUID().toString();
    student=account(Role.STUDENT);admin=account(Role.ADMIN);
  }
  @AfterEach void clean() throws Exception {
    awaitJobs();
    for(int id:owned) {jdbc.update("DELETE FROM notifications WHERE user_id=?",id);jdbc.update("DELETE FROM users WHERE user_id=?",id);}
    owned.clear();
  }
  User account(Role role) {
    var u=new User();String name="recover_"+UUID.randomUUID().toString().replace("-","").substring(0,20);
    u.setUsername(name);u.setEmail(name+"@example.invalid");u.setFullName("Recovery test");u.setRole(role);u.setPasswordHash(encoder.encode("Original123!"));
    u=users.saveAndFlush(u);owned.add(u.getUserId());return u;
  }
  void awaitJobs() throws Exception {
    long deadline=System.nanoTime()+TimeUnit.SECONDS.toNanos(5);
    var pending=(java.util.concurrent.atomic.AtomicInteger)org.springframework.test.util.ReflectionTestUtils.getField(recovery,"outstandingJobs");
    while(pending.get()>0) {if(System.nanoTime()>deadline)throw new AssertionError("Recovery job timeout");Thread.sleep(10);}
  }
  String token(User user) throws Exception {
    recovery.request(user.getEmail(),UUID.randomUUID().toString());awaitJobs();
    var message=delivery.list().stream().filter(mail->mail.recipient().equals(user.getEmail())).findFirst().orElseThrow();
    return message.resetUrl().substring(message.resetUrl().indexOf("#token=")+7);
  }
  MockHttpSession login(User user,String password) throws Exception {
    return (MockHttpSession)mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json")
        .content("{\"username\":\""+user.getUsername()+"\",\"password\":\""+password+"\"}"))
        .andExpect(status().isOk()).andReturn().getRequest().getSession(false);
  }
  @Test void requestIsUniformPrivateAndCsrfProtected() throws Exception {
    var accepted=mvc.perform(post("/api/auth/forgot-password").with(csrf()).with(request->{request.setRemoteAddr(address);return request;})
        .contentType("application/json").content("{\"email\":\""+student.getEmail().toUpperCase(Locale.ROOT)+"\"}"))
        .andExpect(status().isAccepted()).andExpect(header().string("Cache-Control","no-store")).andReturn().getResponse().getContentAsString();
    var unknown=mvc.perform(post("/api/auth/forgot-password").with(csrf()).with(request->{request.setRemoteAddr(address);return request;})
        .contentType("application/json").content("{\"email\":\"unknown_"+address+"@example.invalid\"}"))
        .andExpect(status().isAccepted()).andReturn().getResponse().getContentAsString();assertEquals(accepted,unknown);
    mvc.perform(post("/api/auth/forgot-password").contentType("application/json").content("{\"email\":\""+student.getEmail()+"\"}")).andExpect(status().isForbidden());
    mvc.perform(post("/api/auth/forgot-password").with(csrf()).contentType("application/json").content("{\"email\":\"not-email\"}")).andExpect(status().isBadRequest());
    awaitJobs();assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE user_id=?",Integer.class,student.getUserId()));
    String raw=delivery.list().stream().filter(mail->mail.recipient().equals(student.getEmail())).findFirst().orElseThrow().resetUrl().split("#token=")[1];
    assertEquals(43,raw.length());assertNotEquals(raw,jdbc.queryForObject("SELECT token_hash FROM password_reset_tokens WHERE user_id=?",String.class,student.getUserId()));
    assertEquals(PasswordRecoveryService.hash(raw),jdbc.queryForObject("SELECT token_hash FROM password_reset_tokens WHERE user_id=?",String.class,student.getUserId()));
    mvc.perform(get("/api/demo/recovery-mailbox")).andExpect(status().isUnauthorized());
    mvc.perform(get("/api/demo/recovery-mailbox").with(user(new CustomUserDetails(student)))).andExpect(status().isForbidden());
    var teacher=account(Role.TEACHER);mvc.perform(get("/api/demo/recovery-mailbox").with(user(new CustomUserDetails(teacher)))).andExpect(status().isForbidden());
    mvc.perform(get("/api/demo/recovery-mailbox").with(user(new CustomUserDetails(admin)))).andExpect(status().isOk()).andExpect(header().string("Cache-Control","no-store"));
    recovery.request(student.getEmail(),UUID.randomUUID().toString());awaitJobs();assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE user_id=?",Integer.class,student.getUserId()));
  }
  @Test void resetUsesTokenOnceInvalidatesOldSessionsAndDoesNotLogIn() throws Exception {
    var oldSession=login(student,"Original123!");String token=token(student);
    String body="{\"token\":\""+token+"\",\"newPassword\":\"Changed123!\"}";
    mvc.perform(post("/api/auth/reset-password").contentType("application/json").content(body)).andExpect(status().isForbidden());
    var result=mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType("application/json").content(body))
        .andExpect(status().isOk()).andReturn();
    assertNull(result.getRequest().getSession(false));
    mvc.perform(get("/api/auth/me").session(oldSession)).andExpect(status().isUnauthorized());
    mvc.perform(post("/api/auth/reset-password").with(csrf()).contentType("application/json").content(body)).andExpect(status().isBadRequest());
    mvc.perform(post("/api/auth/login").with(csrf()).contentType("application/json").content("{\"username\":\""+student.getUsername()+"\",\"password\":\"Original123!\"}")).andExpect(status().isUnauthorized());
    assertNotNull(login(student,"Changed123!"));
    assertEquals(1,users.findById(student.getUserId()).orElseThrow().getAuthVersion());
    assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM notifications WHERE user_id=? AND type='PASSWORD_CHANGED'",Integer.class,student.getUserId()));
  }
  @Test void expiredDisabledChangedEmailAndChangedCredentialsInvalidateTokens() throws Exception {
    String expired=token(student);jdbc.update("UPDATE password_reset_tokens SET expires_at=? WHERE user_id=?",LocalDateTime.now(WeeklyGoalService.ZONE).minusSeconds(1),student.getUserId());
    assertEquals(PasswordRecoveryService.INVALID,assertThrows(BadRequestException.class,()->recovery.reset(expired,"Changed123!",address)).getMessage());
    jdbc.update("DELETE FROM password_reset_tokens WHERE user_id=?",student.getUserId());String changed=token(student);
    student.setEmail("changed_"+student.getEmail());users.saveAndFlush(student);assertThrows(BadRequestException.class,()->recovery.reset(changed,"Changed123!",address));
    jdbc.update("DELETE FROM password_reset_tokens WHERE user_id=?",student.getUserId());String locked=token(student);
    student.setIsActive(false);users.saveAndFlush(student);assertThrows(BadRequestException.class,()->recovery.reset(locked,"Changed123!",address));
    jdbc.update("DELETE FROM password_reset_tokens WHERE user_id=?",student.getUserId());recovery.request(student.getEmail(),address);awaitJobs();
    assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE user_id=?",Integer.class,student.getUserId()));
    student.setIsActive(true);users.saveAndFlush(student);String version=token(student);student.setAuthVersion(1);users.saveAndFlush(student);
    assertThrows(BadRequestException.class,()->recovery.reset(version,"Changed123!",address));
    assertThrows(BadRequestException.class,()->recovery.reset("invalid","Changed123!",address));
    assertTrue(encoder.matches("Original123!",users.findById(student.getUserId()).orElseThrow().getPasswordHash()));
  }
  @Test void concurrentResetHasOneWinnerAndAllOtherLinksAreRevoked() throws Exception {
    String one=token(student);jdbc.update("UPDATE password_reset_tokens SET created_at=? WHERE user_id=?",LocalDateTime.now(WeeklyGoalService.ZONE).minusMinutes(2),student.getUserId());
    String two=token(student);assertNotEquals(one,two);
    var pool=Executors.newFixedThreadPool(2);var barrier=new CyclicBarrier(2);
    try {
      Callable<Boolean> reset=()->{barrier.await();try{recovery.reset(two,"Winner123!",UUID.randomUUID().toString());return true;}catch(BadRequestException expected){return false;}};
      var a=pool.submit(reset);var b=pool.submit(reset);assertNotEquals(a.get(10,TimeUnit.SECONDS),b.get(10,TimeUnit.SECONDS));
    }finally{pool.shutdownNow();}
    assertThrows(BadRequestException.class,()->recovery.reset(one,"Wrong123!",address));
    assertTrue(encoder.matches("Winner123!",users.findById(student.getUserId()).orElseThrow().getPasswordHash()));
    assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE user_id=?",Integer.class,student.getUserId()));
  }
  @Test void deliveryFailureRevokesTokenAndResetFailureRollsBackPasswordAndConsumption() throws Exception {
    doThrow(new IllegalStateException("Simulated mail failure")).when(delivery).send(eq(student.getEmail()),anyString(),any());
    recovery.request(student.getEmail(),address);awaitJobs();assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE user_id=?",Integer.class,student.getUserId()));
    reset(delivery);String token=token(student);
    doThrow(new IllegalStateException("Simulated notification failure")).when(notices).save(any(Notification.class));
    assertThrows(IllegalStateException.class,()->recovery.reset(token,"Changed123!",address));
    assertTrue(encoder.matches("Original123!",users.findById(student.getUserId()).orElseThrow().getPasswordHash()));
    assertEquals(1,jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE user_id=?",Integer.class,student.getUserId()));
    reset(notices);recovery.reset(token,"Changed123!",address);
    assertTrue(encoder.matches("Changed123!",users.findById(student.getUserId()).orElseThrow().getPasswordHash()));
  }
  @Test void requestAndResetRateLimitsAreIndependentAndIgnoreForwardedSpoofing() throws Exception {
    for(int i=0;i<10;i++)mvc.perform(post("/api/auth/forgot-password").with(csrf()).with(request->{request.setRemoteAddr(address);return request;})
        .contentType("application/json").content("{\"email\":\"not_found@example.invalid\"}")).andExpect(status().isAccepted());
    mvc.perform(post("/api/auth/forgot-password").with(csrf()).header("X-Forwarded-For","different-client")
        .with(request->{request.setRemoteAddr(address);return request;}).contentType("application/json").content("{\"email\":\""+student.getEmail()+"\"}"))
        .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.success").value(false));
    for(int i=0;i<30;i++)assertThrows(BadRequestException.class,()->recovery.reset("invalid","Changed123!",address));
    var limited=assertThrows(org.springframework.web.server.ResponseStatusException.class,()->recovery.reset("invalid","Changed123!",address));
    assertEquals(429,limited.getStatusCode().value());
  }
  @Test void ambiguousEmailsAndOversizedUtf8PasswordsAreRejectedSafely() throws Exception {
    var duplicate=account(Role.STUDENT);duplicate.setEmail(student.getEmail().toUpperCase(Locale.ROOT));users.saveAndFlush(duplicate);
    recovery.request(student.getEmail(),address);awaitJobs();assertEquals(0,jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens WHERE user_id=?",Integer.class,student.getUserId()));
    users.deleteById(duplicate.getUserId());owned.remove(duplicate.getUserId());
    String token=token(student);assertThrows(BadRequestException.class,()->recovery.reset(token,"ấ".repeat(25),address));
    assertThrows(BadRequestException.class,()->recovery.reset(token," ".repeat(10),address));
    recovery.reset(token,"ấ".repeat(24),address);assertTrue(encoder.matches("ấ".repeat(24),users.findById(student.getUserId()).orElseThrow().getPasswordHash()));
  }
}
