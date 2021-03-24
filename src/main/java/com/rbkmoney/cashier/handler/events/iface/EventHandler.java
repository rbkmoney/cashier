package com.rbkmoney.cashier.handler.events.iface;

import com.rbkmoney.cashier.domain.InvoiceChangeWithMetadata;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;

public interface EventHandler {

    void handle(InvoiceChangeWithMetadata invoiceChangeWithMetadata);

    boolean isApplicable(InvoiceChange invoiceChange);
}
