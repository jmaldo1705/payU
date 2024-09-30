package com.payu.model;

import lombok.Data;

@Data
public class BankResponse {
    private String status;
    private String transactionId;
    private String message;
}