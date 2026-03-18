package com.example.notification_service.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NotificationService {

    @KafkaListener(topics = "notificationTopic", groupId = "notificationId")
    public void handleNotification(String message) {
        // In a real application, this is where you would integrate SendGrid, Twilio, or JavaMailSender
        log.info("===============================================================");
        log.info("RECEIVED EVENT FROM KAFKA: {}", message);
        log.info("Email successfully dispatched to customer!");
        log.info("===============================================================");
    }
}