package com.example.payment_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "t_payments")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String orderId;
    private Double amount;
    private String paymentStatus; // e.g., "SUCCESS" or "FAILED"
}