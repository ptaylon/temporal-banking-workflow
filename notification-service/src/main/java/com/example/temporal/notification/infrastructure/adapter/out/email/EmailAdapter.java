package com.example.temporal.notification.infrastructure.adapter.out.email;

import com.example.temporal.notification.domain.port.out.EmailPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

/**
 * Email adapter for sending email notifications
 * Implements the EmailPort using Spring Mail
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmailAdapter implements EmailPort {

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Override
    public boolean sendEmail(String recipient, String subject, String body) {
        if (!emailEnabled) {
            log.info("[Email disabled] Skipping email to {}: {}", recipient, subject);
            return true; // Return true as we're intentionally not sending
        }

        try {
            log.info("Sending email to {}: {}", recipient, subject);
            
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(recipient);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@banking-demo.com");

            mailSender.send(message);
            
            log.info("Email sent successfully to {}", recipient);
            return true;
            
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", recipient, e.getMessage());
            return false;
        }
    }

    @Override
    public EmailPort.EmailResult sendEmailWithResult(String recipient, String subject, String body) {
        boolean success = sendEmail(recipient, subject, body);
        if (success) {
            return EmailPort.EmailResult.success();
        } else {
            return EmailPort.EmailResult.failure("Failed to send email");
        }
    }
}
