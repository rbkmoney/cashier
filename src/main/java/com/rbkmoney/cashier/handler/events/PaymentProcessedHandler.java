package com.rbkmoney.cashier.handler.events;

import com.rbkmoney.cashier.domain.InvoiceChangeWithMetadata;
import com.rbkmoney.cashier.handler.events.iface.AbstractEventHandler;
import com.rbkmoney.cashier.repository.InvoiceAggregateRepository;
import com.rbkmoney.cashier.repository.ProviderRepository;
import com.rbkmoney.cashier.service.CashRegService;
import com.rbkmoney.damsel.cashreg_processing.CashRegParams;
import com.rbkmoney.damsel.payment_processing.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class PaymentProcessedHandler extends AbstractEventHandler {

    private final InvoiceAggregateRepository invoiceAggregateRepository;
    private final ProviderRepository providerRepository;
    private final CashRegService cashRegService;

    public PaymentProcessedHandler(
            @Value("${events.path.payment-processed}") String path,
            InvoiceAggregateRepository repository,
            ProviderRepository providerRepository,
            CashRegService cashRegService) {
        super(path);
        this.invoiceAggregateRepository = repository;
        this.providerRepository = providerRepository;
        this.cashRegService = cashRegService;
    }

    @Override
    public void handle(InvoiceChangeWithMetadata invoiceChangeWithMetadata) {
        String invoiceId = invoiceChangeWithMetadata.getInvoiceId();
        long eventId = invoiceChangeWithMetadata.getEventId();

        log.debug("Handling new PaymentProcessed event: invoiceId={}, eventId={}...", invoiceId, eventId);

        Invoice aggregate = invoiceAggregateRepository.findByInvoiceIdAndEventId(
                invoiceId,
                eventId);

        String providerId = providerRepository.findBy();

        CashRegParams debitForInvoice = cashRegService.debitForInvoice(
                providerId,
                aggregate);

        cashRegService.send(debitForInvoice);

        log.debug("Finished handling PaymentProcessed event: invoiceId={}, eventId={}", invoiceId, eventId);
    }
}
