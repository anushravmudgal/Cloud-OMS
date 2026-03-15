package com.example.order_service.service;

import com.example.order_service.dto.OrderRequest;
import com.example.order_service.entity.Order;
import com.example.order_service.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class) // Initializes Mocks automatically
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository; // Mocks the DB interface

    @InjectMocks
    private OrderService orderService; // Injects the mock repo into the service

    @Test
    void shouldPlaceOrderSuccessfully() {
        // Arrange: Prepare our input data
        OrderRequest request = new OrderRequest("IPHONE_15", new BigDecimal("1200"), 1);

        // Act: Call the service method
        orderService.placeOrder(request);

        // Assert: Verify the logic
        // We use an ArgumentCaptor to "catch" the Order object sent to the repository
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderRepository, times(1)).save(orderCaptor.capture());

        Order savedOrder = orderCaptor.getValue();

        assertNotNull(savedOrder.getOrderNumber()); // Verifies UUID was generated
        assertEquals("IPHONE_15", savedOrder.getSkuCode());
        assertEquals(new BigDecimal("1200"), savedOrder.getPrice());
        assertTrue(savedOrder.getOrderNumber().length() > 0);
    }
}