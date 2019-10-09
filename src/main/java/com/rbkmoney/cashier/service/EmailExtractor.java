package com.rbkmoney.cashier.service;

import com.rbkmoney.damsel.domain.*;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class EmailExtractor {

    String extract(List<com.rbkmoney.damsel.payment_processing.InvoicePayment> payments) {
        return payments
                .stream()
                .map(com.rbkmoney.damsel.payment_processing.InvoicePayment::getPayment)
                .filter(InvoicePayment::isSetPayer)
                .findAny()
                .map(InvoicePayment::getPayer)
                .flatMap(this::extractEmail)
                .orElse(null);
    }

    private Optional<String> extractEmail(Payer payer) {
        if (payer.isSetPaymentResource()) {
            return Optional.of(payer.getPaymentResource())
                    .map(PaymentResourcePayer::getContactInfo)
                    .map(ContactInfo::getEmail);
        }

        if (payer.isSetCustomer()) {
            return Optional.of(payer.getCustomer())
                    .map(CustomerPayer::getContactInfo)
                    .map(ContactInfo::getEmail);
        }

        if (payer.isSetRecurrent()) {
            return Optional.of(payer.getRecurrent())
                    .map(RecurrentPayer::getContactInfo)
                    .map(ContactInfo::getEmail);
        }

        return Optional.empty();
    }
}
