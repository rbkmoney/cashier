package com.rbkmoney.cashier.repository;

import com.rbkmoney.damsel.payment_processing.Invoice;
import org.springframework.stereotype.Repository;

@Repository
public class InvoiceAggregateRepository {

    // TODO [a.romanov]: implementation
    public Invoice findByInvoiceIdAndEventId(String invoiceId, long eventId) {
        return new Invoice();
    }
}
