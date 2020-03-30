package com.rbkmoney.cashier.handler.events;

import com.rbkmoney.cashier.domain.InvoiceChangeWithMetadata;
import com.rbkmoney.cashier.handler.events.iface.AbstractEventHandler;
import com.rbkmoney.cashier.repository.InvoiceAggregateRepository;
import com.rbkmoney.cashier.repository.ProviderRepository;
import com.rbkmoney.cashier.service.CashRegService;
import com.rbkmoney.damsel.cashreg_processing.CashRegParams;
import com.rbkmoney.damsel.domain.InvoicePaymentRefund;
import com.rbkmoney.damsel.payment_processing.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
@Service
public class RefundSucceededHandler extends AbstractEventHandler {

    private final InvoiceAggregateRepository invoiceAggregateRepository;
    private final ProviderRepository providerRepository;
    private final CashRegService cashRegService;

    public RefundSucceededHandler(
            @Value("${events.path.refund-succeeded}") String path,
            InvoiceAggregateRepository invoiceAggregateRepository,
            ProviderRepository providerRepository,
            CashRegService cashRegService) {
        super(path);
        this.invoiceAggregateRepository = invoiceAggregateRepository;
        this.providerRepository = providerRepository;
        this.cashRegService = cashRegService;
    }

    @Override
    public void handle(InvoiceChangeWithMetadata invoiceChangeWithMetadata) {
        String invoiceId = invoiceChangeWithMetadata.getInvoiceId();
        long eventId = invoiceChangeWithMetadata.getEventId();

        log.debug("Handling new RefundSucceeded event: invoiceId={}, eventId={}...", invoiceId, eventId);

        Invoice aggregate = invoiceAggregateRepository.findByInvoiceIdAndEventId(
                invoiceId,
                eventId);

        String providerId = providerRepository.findBy();

        String currentRefundId = invoiceChangeWithMetadata
                .getInvoiceChange()
                .getInvoicePaymentChange()
                .getPayload()
                .getInvoicePaymentRefundChange()
                .getId();

        log.debug("Looking if current refund is partial...");
        Optional<InvoicePaymentRefund> currentPartialRefund = findPartialRefundById(
                aggregate.getPayments(),
                currentRefundId);

        if (currentPartialRefund.isPresent()) {
            log.debug("Current refund is partial");
            CashRegParams debitForPartialRefund = cashRegService.debitForPartialRefund(
                    providerId,
                    aggregate,
                    currentPartialRefund.get());

            cashRegService.send(debitForPartialRefund);
        } else {
            log.debug("Current refund is NOT partial");
        }

        CashRegParams refundDebit = refundDebitForPreviousPartialRefund(providerId, aggregate, currentRefundId)
                .orElse(cashRegService.refundDebitForInvoice(providerId, aggregate));

        cashRegService.send(refundDebit);

        log.debug("Finished handling RefundSucceeded event: invoiceId={}, eventId={}", invoiceId, eventId);
    }

    private Optional<CashRegParams> refundDebitForPreviousPartialRefund(
            String providerId,
            Invoice aggregate,
            String currentRefundId) {
        log.debug("Looking for previous successful refunds...");
        List<InvoicePayment> payments = aggregate.getPayments();
        Optional<String> previousRefundId = findPreviousSuccessfulRefundId(
                payments,
                currentRefundId);

        if (previousRefundId.isEmpty()) {
            log.debug("No previous successful refunds were found");
            return Optional.empty();
        }

        Optional<InvoicePaymentRefund> previousPartialRefund = findPartialRefundById(
                payments,
                previousRefundId.get());

        if (previousPartialRefund.isEmpty()) {
            log.debug("Previous successful refund was NOT partial");
            return Optional.empty();
        }

        log.debug("Previous successful partial refund was found");
        return Optional.of(
                cashRegService.refundDebitForPreviousPartialRefund(
                        providerId,
                        aggregate,
                        previousPartialRefund.get()));
    }

    private Optional<String> findPreviousSuccessfulRefundId(
            List<InvoicePayment> payments,
            String currentRefundId) {
        return payments.stream()
                .flatMap(this::unwrapRefund)
                .filter(refund -> !refund.getId().equals(currentRefundId))
                .filter(InvoicePaymentRefund::isSetStatus)
                .filter(refund -> refund.getStatus().isSetSucceeded())
                .map(InvoicePaymentRefund::getId)
                .reduce((first, second) -> second);
    }

    private Optional<InvoicePaymentRefund> findPartialRefundById(
            List<InvoicePayment> payments,
            String refundId) {
        return payments.stream()
                .flatMap(this::unwrapRefund)
                .filter(refund -> refund.getId().equals(refundId))
                .filter(InvoicePaymentRefund::isSetCart)
                .findAny();
    }

    private Stream<InvoicePaymentRefund> unwrapRefund(InvoicePayment payment) {
        return payment.getRefunds()
                .stream()
                .map(com.rbkmoney.damsel.payment_processing.InvoicePaymentRefund::getRefund);
    }
}
