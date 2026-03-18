package com.example.order_service.config;

import io.micrometer.observation.ObservationRegistry;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestClient;

@Configuration
public class WebClientConfig {

    // 1. Primary builder for Eureka. We explicitly attach the Micrometer tracing registry!
    @Bean
    @Primary
    public RestClient.Builder normalRestClientBuilder(ObservationRegistry observationRegistry) {
        return RestClient.builder()
                .observationRegistry(observationRegistry);
    }

    // 2. Our Load-Balanced builder. We attach the exact same tracing registry here!
    @Bean
    @LoadBalanced
    public RestClient.Builder loadBalancedRestClientBuilder(ObservationRegistry observationRegistry) {
        return RestClient.builder()
                .observationRegistry(observationRegistry);
    }

    // 3. Cleanly inject the load-balanced builder for our Order Service to use
    @Bean
    public RestClient restClient(@Qualifier("loadBalancedRestClientBuilder") RestClient.Builder builder) {
        return builder.build();
    }
}