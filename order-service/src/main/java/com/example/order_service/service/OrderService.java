package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.entity.Order;
import com.example.order_service.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class OrderService {

    private final OrderRepository orderRepository;
    private final RestClient restClient;
    private final KafkaTemplate<String, String> kafkaTemplate;


    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    public String placeOrder(OrderRequest orderRequest) {

        log.info("Checking Inventory for SKU: {}", orderRequest.getSkuCode());


        Boolean inStock = restClient.get()
                .uri("http://inventory-service/api/inventory/" + orderRequest.getSkuCode())
                .retrieve()
                .body(Boolean.class);

        if (inStock != null && inStock) {
            Order order = Order.builder()
                    .orderNumber(UUID.randomUUID().toString())
                    .skuCode(orderRequest.getSkuCode())
                    .price(orderRequest.getPrice())
                    .quantity(orderRequest.getQuantity())
                    .status("PENDING") // 1. Start as PENDING
                    .build();

            orderRepository.save(order);

            // 2. Trigger the Saga! Tell the Payment Service to charge the card.
            log.info("Order PENDING. Sending to Payment Service...");
            kafkaTemplate.send("orderTopic", order.getOrderNumber());

            return "Order received! Processing payment...";
        } else {
            throw new IllegalArgumentException("Product is not in stock, please try again later.");
        }
    }


    public String fallbackMethod(OrderRequest orderRequest, RuntimeException runtimeException) {

        if (runtimeException instanceof IllegalArgumentException) {
            log.warn("Business rule triggered: {}", runtimeException.getMessage());
            return runtimeException.getMessage();
        }


        log.error("Inventory service is down! Executing fallback. Error: {}", runtimeException.getMessage());
        return "Oops! Something went wrong with our inventory system. Please order after some time!";
    }


    @KafkaListener(topics = "paymentTopic", groupId = "orderGroup")
    public void handlePaymentResult(String message) {
        log.info("========================================");
        log.info("SAGA: Received Payment Result -> {}", message);


        String[] parts = message.split(",");
        String orderId = parts[0];
        String paymentStatus = parts[1];


        Order order = orderRepository.findByOrderNumber(orderId);

        if (order != null) {
            if ("SUCCESS".equals(paymentStatus)) {
                order.setStatus("CONFIRMED");
                log.info("SAGA: Order {} is officially CONFIRMED!", orderId);

                kafkaTemplate.send("notificationTopic", "Payment Received! Order Confirmed: " + orderId);

            } else {
                order.setStatus("CANCELLED");
                log.error("SAGA: Payment failed. Order {} is CANCELLED (Compensating Transaction Triggered)", orderId);

            }
            orderRepository.save(order);
        }
        log.info("========================================");
    }

}