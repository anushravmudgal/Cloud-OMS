package com.example.payment_service.service;


import com.example.payment_service.entity.Payment;
import com.example.payment_service.repository.PaymentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.Random;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentProcessingService {

    private final PaymentRepository paymentRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    @KafkaListener(topics = "orderTopic", groupId = "paymentGroup")
    public void processPayment(String orderId) {
        log.info("========================================");
        log.info("RECEIVED ORDER FOR PAYMENT: {}", orderId);

        boolean isPaymentSuccessful = new Random().nextDouble() > 0.2;
        String status = isPaymentSuccessful ? "SUCCESS" : "FAILED";


        Payment payment = Payment.builder()
                .orderId(orderId)
                .amount(999.99)
                .paymentStatus(status)
                .build();

        paymentRepository.save(payment);
        log.info("Payment saved to database with status: {}", status);

        String paymentResultMessage = orderId + "," + status;
        kafkaTemplate.send("paymentTopic", paymentResultMessage);

        log.info("Sent result back to Kafka: {}", paymentResultMessage);
        log.info("========================================");
    }
}