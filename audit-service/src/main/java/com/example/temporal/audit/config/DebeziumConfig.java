package com.example.temporal.audit.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DebeziumConfig {

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}