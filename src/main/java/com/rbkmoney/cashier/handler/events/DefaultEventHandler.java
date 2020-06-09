package com.rbkmoney.cashier.handler.events;


import com.rbkmoney.cashier.domain.InvoiceChangeWithMetadata;
import com.rbkmoney.cashier.handler.events.iface.EventHandler;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class DefaultEventHandler implements EventHandler {

    @Override
    public void handle(InvoiceChangeWithMetadata invoiceChangeWithMetadata) {
        // NOTE: skip events if they are not being processed
        log.debug("No handler was found for invoice change=" + invoiceChangeWithMetadata.getInvoiceChange());
    }

    @Override
    public boolean isApplicable(InvoiceChange invoiceChange) {
        return false;
    }
}
