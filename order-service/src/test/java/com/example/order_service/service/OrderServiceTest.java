package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.entity.Order;
import com.example.order_service.repository.OrderRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private RestClient restClient;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Place Order - Success Path (Inventory OK)")
    void placeOrder_Success() {
        // --- GIVEN ---
        OrderRequest request = new OrderRequest();
        request.setSkuCode("MACBOOK_PRO");
        request.setPrice(BigDecimal.valueOf(2000));
        request.setQuantity(1);


        when(restClient.get()
                .uri(anyString())
                .retrieve()
                .body(Boolean.class)).thenReturn(true);


        String result = orderService.placeOrder(request);


        assertThat(result).isEqualTo("Order received! Processing payment...");


        verify(orderRepository, times(1)).save(any(Order.class));
        verify(kafkaTemplate, times(1)).send(eq("orderTopic"), anyString());
    }

    @Test
    @DisplayName("Place Order - Failure Path (Out of Stock)")
    void placeOrder_OutOfStock() {

        OrderRequest request = new OrderRequest();
        request.setSkuCode("IPHONE_15");


        when(restClient.get()
                .uri(anyString())
                .retrieve()
                .body(Boolean.class)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderService.placeOrder(request);
        });

        assertThat(exception.getMessage()).isEqualTo("Product is not in stock, please try again later.");


        verify(orderRepository, never()).save(any(Order.class));
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    @Test
    @DisplayName("Saga Pattern: Handle Successful Payment")
    void handlePaymentResult_Success() {

        String orderId = UUID.randomUUID().toString();
        String message = orderId + ",SUCCESS";

        Order mockOrder = Order.builder()
                .orderNumber(orderId)
                .status("PENDING")
                .build();

        when(orderRepository.findByOrderNumber(orderId)).thenReturn(mockOrder);

        orderService.handlePaymentResult(message);


        assertThat(mockOrder.getStatus()).isEqualTo("CONFIRMED");
        verify(orderRepository, times(1)).save(mockOrder);

        verify(kafkaTemplate, times(1)).send(eq("notificationTopic"), anyString());
    }

    @Test
    @DisplayName("Saga Pattern: Handle Failed Payment (Compensating Transaction)")
    void handlePaymentResult_Failure() {

        String orderId = UUID.randomUUID().toString();
        String message = orderId + ",FAILED";

        Order mockOrder = Order.builder()
                .orderNumber(orderId)
                .status("PENDING")
                .build();

        when(orderRepository.findByOrderNumber(orderId)).thenReturn(mockOrder);


        orderService.handlePaymentResult(message);


        assertThat(mockOrder.getStatus()).isEqualTo("CANCELLED");
        verify(orderRepository, times(1)).save(mockOrder);

        verify(kafkaTemplate, never()).send(eq("notificationTopic"), anyString());
    }
}