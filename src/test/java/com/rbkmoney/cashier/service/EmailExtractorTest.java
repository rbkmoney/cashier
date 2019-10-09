package com.rbkmoney.cashier.service;

import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertNull;

public class EmailExtractorTest {

    private static final String TEST_EMAIL = "TEST_EMAIL";

    private EmailExtractor emailExtractor;

    @Before
    public void setUp() {
        emailExtractor = new EmailExtractor();
    }

    @Test
    public void shouldReturnNullIfNoEmailWasFound() {
        // Given
        InvoicePayment payment = new InvoicePayment()
                .setPayment(new com.rbkmoney.damsel.domain.InvoicePayment()
                        .setPayer(new Payer()));

        // When
        String email = emailExtractor.extract(List.of(payment));

        // Then
        assertNull(email);
    }

    @Test
    public void shouldExtractEmailFromPaymentResource() {
        // Given
        InvoicePayment payment = new InvoicePayment()
                .setPayment(new com.rbkmoney.damsel.domain.InvoicePayment()
                        .setPayer(Payer.payment_resource(
                                new PaymentResourcePayer()
                                        .setContactInfo(new ContactInfo()
                                                .setEmail(TEST_EMAIL)))));

        // When
        String email = emailExtractor.extract(List.of(payment));

        // Then
        assertThat(email).isEqualTo(TEST_EMAIL);
    }

    @Test
    public void shouldExtractEmailFromCustomer() {
        // Given
        InvoicePayment payment = new InvoicePayment()
                .setPayment(new com.rbkmoney.damsel.domain.InvoicePayment()
                        .setPayer(Payer.customer(
                                new CustomerPayer()
                                        .setContactInfo(new ContactInfo()
                                                .setEmail(TEST_EMAIL)))));

        // When
        String email = emailExtractor.extract(List.of(payment));

        // Then
        assertThat(email).isEqualTo(TEST_EMAIL);
    }

    @Test
    public void shouldExtractEmailFromRecurrent() {
        // Given
        InvoicePayment payment = new InvoicePayment()
                .setPayment(new com.rbkmoney.damsel.domain.InvoicePayment()
                        .setPayer(Payer.recurrent(
                                new RecurrentPayer()
                                        .setContactInfo(new ContactInfo()
                                                .setEmail(TEST_EMAIL)))));

        // When
        String email = emailExtractor.extract(List.of(payment));

        // Then
        assertThat(email).isEqualTo(TEST_EMAIL);
    }
}