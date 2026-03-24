package com.example.temporal.transfer.infrastructure.adapter.out.http;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LockAccountsRequest {
    private String sourceAccountNumber;
    private String destinationAccountNumber;
}
