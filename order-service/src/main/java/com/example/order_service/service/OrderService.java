package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.entity.Order;
import com.example.order_service.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
                .uri("http://localhost:8082/api/inventory/" + orderRequest.getSkuCode())
                .retrieve()
                .body(Boolean.class);

        if (inStock != null && inStock) {
            Order order = Order.builder()
                    .orderNumber(UUID.randomUUID().toString())
                    .skuCode(orderRequest.getSkuCode())
                    .price(orderRequest.getPrice())
                    .quantity(orderRequest.getQuantity())
                    .build();

            orderRepository.save(order);


            log.info("Order saved! Sending Kafka event...");
            kafkaTemplate.send("notificationTopic", "Order Placed Successfully - " + order.getOrderNumber());

            return "Order placed successfully!";
        } else {
            throw new IllegalArgumentException("Product is not in stock, please try again later.");
        }
    }


    public String fallbackMethod(OrderRequest orderRequest, RuntimeException runtimeException) {
        log.error("Inventory service is down! Executing fallback. Error: {}", runtimeException.getMessage());
        return "Oops! Something went wrong with our inventory system. Please order after some time!";
    }
}