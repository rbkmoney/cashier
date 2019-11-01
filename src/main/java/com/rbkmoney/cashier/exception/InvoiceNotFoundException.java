package com.rbkmoney.cashier.exception;

public class InvoiceNotFoundException extends RuntimeException {

    public InvoiceNotFoundException(Throwable cause) {
        super(cause);
    }
}
