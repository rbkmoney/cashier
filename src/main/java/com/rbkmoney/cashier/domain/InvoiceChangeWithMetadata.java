package com.rbkmoney.cashier.domain;

import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import lombok.Builder;
import lombok.Getter;

@Builder
@Getter
public class InvoiceChangeWithMetadata {

    private final String invoiceId;
    private final long eventId;
    private final InvoiceChange invoiceChange;
}
