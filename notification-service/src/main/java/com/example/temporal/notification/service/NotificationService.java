package com.example.temporal.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {
    
    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    public void sendTransferInitiatedNotification(Long transferId) {
        if (!emailEnabled) {
            log.info("[Email disabled] Skipping transfer initiated email for transfer: {}", transferId);
            return;
        }
        log.info("Sending transfer initiated notification for transfer: {}", transferId);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("Transfer Initiated");
        message.setText("Your transfer with ID " + transferId + " has been initiated.");
        // In a real application, we would get the user's email from a user service
        message.setTo("user@example.com");
        mailSender.send(message);
    }

    public void sendTransferCompletedNotification(Long transferId) {
        if (!emailEnabled) {
            log.info("[Email disabled] Skipping transfer completed email for transfer: {}", transferId);
            return;
        }
        log.info("Sending transfer completed notification for transfer: {}", transferId);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("Transfer Completed");
        message.setText("Your transfer with ID " + transferId + " has been completed successfully.");
        message.setTo("user@example.com");
        mailSender.send(message);
    }

    public void sendTransferFailedNotification(Long transferId, String reason) {
        if (!emailEnabled) {
            log.info("[Email disabled] Skipping transfer failed email for transfer: {} - reason: {}", transferId, reason);
            return;
        }
        log.info("Sending transfer failed notification for transfer: {}", transferId);
        SimpleMailMessage message = new SimpleMailMessage();
        message.setSubject("Transfer Failed");
        message.setText("Your transfer with ID " + transferId + " has failed. Reason: " + reason);
        message.setTo("user@example.com");
        mailSender.send(message);
    }
}