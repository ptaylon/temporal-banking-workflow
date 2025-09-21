package com.example.temporal.validation.controller;

import com.example.temporal.common.dto.TransferRequest;
import com.example.temporal.validation.service.ValidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/validations")
@RequiredArgsConstructor
public class ValidationController {

    private final ValidationService validationService;

    @PostMapping("/transfer")
    public ResponseEntity<Void> validateTransfer(@RequestBody final TransferRequest request) {
        validationService.validateTransfer(request);
        return ResponseEntity.ok().build();
    }
}