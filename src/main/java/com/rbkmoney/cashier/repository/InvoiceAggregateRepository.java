package com.rbkmoney.cashier.repository;

import com.rbkmoney.cashier.exception.InvoiceNotFoundException;
import com.rbkmoney.damsel.payment_processing.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.stereotype.Repository;

@Slf4j
@Repository
@RequiredArgsConstructor
public class InvoiceAggregateRepository {

    private final InvoicingSrv.Iface invoicingClient;
    private final UserInfo invoicingAdmin;

    public Invoice findByInvoiceIdAndEventId(String invoiceId, Long eventId) {
        try {
            log.debug("Looking for aggregate with invoiceId={}", invoiceId);
            return invoicingClient.get(
                    invoicingAdmin,
                    invoiceId,
                    new EventRange()
                            .setLimit(eventId.intValue()));
        } catch (TException e) {
            throw new InvoiceNotFoundException(e);
        }
    }
}
