package com.example.course_management;

import static org.junit.jupiter.api.Assertions.*;
import com.example.course_management.config.PasswordRecoverySettings;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.mail.javamail.JavaMailSender;

class PasswordRecoveryConfigurationTests {
  private PasswordRecoverySettings settings(String mode,String url,String from,boolean demo,JavaMailSender sender) {
    var factory=new StaticListableBeanFactory();if(sender!=null)factory.addBean("mailSender",sender);
    return new PasswordRecoverySettings(mode,url,from,demo,factory.getBeanProvider(JavaMailSender.class));
  }
  @Test void demoMailboxRequiresDemoAndTrustedConfiguredBaseUrl() {
    assertThrows(IllegalArgumentException.class,()->settings("demo","http://127.0.0.1:8080","",false,null));
    for(String url:new String[]{"http://evil.example","https://user@course.example","https://course.example?redirect=x","https://course.example#x","https://course.example/other","javascript:alert(1)"})
      assertThrows(IllegalArgumentException.class,()->settings("demo",url,"",true,null));
    var enabled=settings("demo","http://127.0.0.1:8080/","",true,null);assertTrue(enabled.demoMailbox());assertEquals("http://127.0.0.1:8080/password-recovery.html#token=test",enabled.resetUrl("test"));
  }
  @Test void smtpRequiresSenderHttpsAndValidFromWhileDefaultIsDisabled() {
    assertFalse(settings("disabled","","",false,null).enabled());
    assertThrows(IllegalArgumentException.class,()->settings("smtp","https://course.example","mail@course.example",false,null));
    var sender=org.mockito.Mockito.mock(JavaMailSender.class);
    assertThrows(IllegalArgumentException.class,()->settings("smtp","http://course.example","mail@course.example",false,sender));
    assertThrows(IllegalArgumentException.class,()->settings("smtp","https://course.example","bad\r\nfrom",false,sender));
    assertTrue(settings("smtp","https://course.example","mail@course.example",false,sender).enabled());
    assertThrows(IllegalArgumentException.class,()->settings("unexpected","","",false,null));
  }
  @Test void smtpUsesConfiguredMailSenderAndNeverExposesDemoMailbox() {
    var sender=org.mockito.Mockito.mock(JavaMailSender.class);
    var config=settings("smtp","https://course.example","mail@course.example",false,sender);
    var factory=new StaticListableBeanFactory();factory.addBean("mailSender",sender);
    var delivery=new com.example.course_management.service.PasswordRecoveryDelivery(config,factory.getBeanProvider(JavaMailSender.class));
    String url=config.resetUrl("A".repeat(43));
    delivery.send("student@course.example",url,java.time.LocalDateTime.now().plusMinutes(15));
    var message=org.mockito.ArgumentCaptor.forClass(org.springframework.mail.SimpleMailMessage.class);
    org.mockito.Mockito.verify(sender).send(message.capture());
    assertEquals("mail@course.example",message.getValue().getFrom());assertArrayEquals(new String[]{"student@course.example"},message.getValue().getTo());
    assertTrue(message.getValue().getText().contains(url));
    assertThrows(com.example.course_management.exception.ResourceNotFoundException.class,delivery::list);
  }
}
