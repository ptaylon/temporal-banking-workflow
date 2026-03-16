package com.example.temporal.validation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication
@EnableFeignClients
@EntityScan(basePackages = "com.example.temporal.validation.entity")
@EnableJpaRepositories(basePackages = "com.example.temporal.validation.repository")
public class ValidationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ValidationServiceApplication.class, args);
    }
}