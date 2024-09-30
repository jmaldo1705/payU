package com.payu.exception;

public class BankDeclinedException extends RuntimeException {
    public BankDeclinedException(String message) {
        super(message);
    }
}
