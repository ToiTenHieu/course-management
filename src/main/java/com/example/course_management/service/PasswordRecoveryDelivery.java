package com.example.course_management.service;

import com.example.course_management.config.PasswordRecoverySettings;
import com.example.course_management.exception.ResourceNotFoundException;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class PasswordRecoveryDelivery {
  public record Message(String id,String recipient,String subject,String body,String resetUrl,LocalDateTime expiresAt,LocalDateTime createdAt) {}
  private final PasswordRecoverySettings settings;
  private final ObjectProvider<JavaMailSender> sender;
  private final Deque<Message> mailbox=new ArrayDeque<>();
  public PasswordRecoveryDelivery(PasswordRecoverySettings settings,ObjectProvider<JavaMailSender> sender) {this.settings=settings;this.sender=sender;}
  public synchronized void send(String email,String resetUrl,LocalDateTime expiresAt) {
    String subject="Đặt lại mật khẩu Course Management";
    String body="Bạn vừa yêu cầu đặt lại mật khẩu. Mở liên kết sau trong 15 phút:\n"+resetUrl
        +"\n\nLiên kết chỉ dùng một lần. Sau khi đổi mật khẩu, các phiên cũ sẽ hết hiệu lực. Nếu bạn không yêu cầu, hãy bỏ qua thư này.";
    if(settings.demoMailbox()) {
      prune();if(mailbox.size()>=100)mailbox.removeLast();
      mailbox.addFirst(new Message(UUID.randomUUID().toString(),email,subject,body,resetUrl,expiresAt,LocalDateTime.now(WeeklyGoalService.ZONE)));
    } else {
      var message=new SimpleMailMessage();message.setFrom(settings.from());message.setTo(email);message.setSubject(subject);message.setText(body);
      Objects.requireNonNull(sender.getIfAvailable(),"SMTP chưa được cấu hình").send(message);
    }
  }
  public synchronized List<Message> list() {
    if(!settings.demoMailbox())throw new ResourceNotFoundException("Hộp thư thử nghiệm không được bật");
    prune();return List.copyOf(mailbox);
  }
  private void prune() {var now=LocalDateTime.now(WeeklyGoalService.ZONE);mailbox.removeIf(message->!message.expiresAt().isAfter(now));}
}
