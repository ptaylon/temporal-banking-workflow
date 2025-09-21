package com.example.temporal.transfer.config;

import feign.codec.ErrorDecoder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FeignConfig {

    @Bean
    public ErrorDecoder errorDecoder() {
        return new FeignErrorDecoder();
    }
}

class FeignErrorDecoder implements ErrorDecoder {
    private final ErrorDecoder defaultErrorDecoder = new Default();

    @Override
    public Exception decode(String methodKey, feign.Response response) {
        if (response.status() >= 400 && response.status() <= 499) {
            // Handle client errors
            return new RuntimeException("Client error when calling service: " + response.reason());
        }
        if (response.status() >= 500 && response.status() <= 599) {
            // Handle server errors
            return new RuntimeException("Service unavailable: " + response.reason());
        }
        return defaultErrorDecoder.decode(methodKey, response);
    }
}