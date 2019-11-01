package com.rbkmoney.cashier.exception;

public class ProviderNotFoundException extends RuntimeException {

    public ProviderNotFoundException(String message) {
        super(message);
    }
}
