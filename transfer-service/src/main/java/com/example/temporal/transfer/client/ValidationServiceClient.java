package com.example.temporal.transfer.client;

import com.example.temporal.common.dto.TransferRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "validation-service", url = "${service.validation.url}")
public interface ValidationServiceClient {
    
    @PostMapping("/api/validations/transfer")
    void validateTransfer(@RequestBody TransferRequest request);
}