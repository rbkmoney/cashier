package com.rbkmoney.cashier.handler.events;

import com.rbkmoney.cashier.domain.CashRegister;
import com.rbkmoney.cashier.domain.InvoiceChangeWithMetadata;
import com.rbkmoney.cashier.handler.events.iface.AbstractEventHandler;
import com.rbkmoney.cashier.repository.CashRegisterRepository;
import com.rbkmoney.cashier.repository.InvoiceAggregateRepository;
import com.rbkmoney.cashier.service.CashregService;
import com.rbkmoney.cashier.service.ReceiptFactory;
import com.rbkmoney.damsel.cashreg.processing.ReceiptParams;
import com.rbkmoney.damsel.domain.InvoicePaymentCaptured;
import com.rbkmoney.damsel.payment_processing.Invoice;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class PaymentCapturedHandler extends AbstractEventHandler {

    private final InvoiceAggregateRepository invoiceAggregateRepository;
    private final CashRegisterRepository cashRegisterRepository;
    private final ReceiptFactory receiptFactory;
    private final CashregService cashregService;

    public PaymentCapturedHandler(
            @Value("${events.path.payment-captured}") String path,
            InvoiceAggregateRepository invoiceAggregateRepository,
            CashRegisterRepository cashRegisterRepository,
            ReceiptFactory receiptFactory,
            CashregService cashregService) {
        super(path);
        this.invoiceAggregateRepository = invoiceAggregateRepository;
        this.cashRegisterRepository = cashRegisterRepository;
        this.receiptFactory = receiptFactory;
        this.cashregService = cashregService;
    }

    @Override
    public void handle(InvoiceChangeWithMetadata invoiceChangeWithMetadata) {
        String invoiceId = invoiceChangeWithMetadata.getInvoiceId();
        long eventId = invoiceChangeWithMetadata.getEventId();

        log.debug("Handling new PaymentCaptured event: invoiceId={}, eventId={}...", invoiceId, eventId);

        InvoicePaymentCaptured capturedPayment = invoiceChangeWithMetadata
                .getInvoiceChange()
                .getInvoicePaymentChange()
                .getPayload()
                .getInvoicePaymentStatusChanged()
                .getStatus()
                .getCaptured();

        log.debug("Looking for partial capture...");
        if (!capturedPayment.isSetCart()) {
            log.debug("Skip event: no partial capture was found");
            return;
        }

        Invoice aggregate = invoiceAggregateRepository.findByInvoiceIdAndEventId(
                invoiceId,
                eventId);

        List<CashRegister> cashRegisters = cashRegisterRepository.findByPartyIdAndShopId(
                aggregate.getInvoice().getOwnerId(),
                aggregate.getInvoice().getShopId());

        ReceiptParams refundDebitForInvoice = receiptFactory.refundDebitForInvoice(
                cashRegisters,
                aggregate,
                eventId);

        ReceiptParams debitForPartialCapture = receiptFactory.debitForPartialCapture(
                cashRegisters,
                aggregate,
                eventId,
                capturedPayment);

        cashregService.send(
                refundDebitForInvoice,
                debitForPartialCapture);

        log.debug("Finished handling PaymentCaptured event: invoiceId={}, eventId={}", invoiceId, eventId);
    }
}
