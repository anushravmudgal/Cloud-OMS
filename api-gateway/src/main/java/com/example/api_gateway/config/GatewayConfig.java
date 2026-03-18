package com.example.api_gateway.config;

import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GatewayConfig {

    @Bean
    public RouteLocator gatewayRoutes(RouteLocatorBuilder builder) {
        return builder.routes()
                // Route 1: Order Service
                .route("order-service", r -> r.path("/api/orders/**")
                        .uri("lb://ORDER-SERVICE"))

                // Route 2: Inventory Service
                .route("inventory-service", r -> r.path("/api/inventory/**")
                        .uri("lb://INVENTORY-SERVICE"))
                .build();
    }
}
