package com.rbkmoney.cashier.handler.events;

import com.rbkmoney.cashier.domain.InvoiceChangeWithMetadata;
import com.rbkmoney.cashier.repository.CashRegisterRepository;
import com.rbkmoney.cashier.repository.InvoiceAggregateRepository;
import com.rbkmoney.cashier.service.CashregService;
import com.rbkmoney.cashier.service.ReceiptFactory;
import com.rbkmoney.damsel.cashreg.processing.ReceiptParams;
import com.rbkmoney.damsel.domain.InvoiceCart;
import com.rbkmoney.damsel.domain.InvoicePaymentRefundStatus;
import com.rbkmoney.damsel.domain.InvoicePaymentRefundSucceeded;
import com.rbkmoney.damsel.payment_processing.*;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.mockito.Mockito.*;

public class RefundSucceededHandlerTest {

    private InvoiceAggregateRepository invoiceAggregateRepository;
    private ReceiptFactory receiptFactory;
    private CashregService cashregService;

    private RefundSucceededHandler handler;

    @Before
    public void setUp() {
        invoiceAggregateRepository = mock(InvoiceAggregateRepository.class);
        receiptFactory = mock(ReceiptFactory.class);
        cashregService = mock(CashregService.class);
        CashRegisterRepository cashRegisterRepository = mock(CashRegisterRepository.class);

        when(cashRegisterRepository.findByPartyIdAndShopId(anyString(), anyString()))
                .thenReturn(List.of());

        handler = new RefundSucceededHandler(
                "",
                invoiceAggregateRepository,
                cashRegisterRepository,
                receiptFactory,
                cashregService);
    }

    @Test
    public void shouldSendRefundDebitForInvoice() {
        // Given
        InvoiceChange invoiceChange = InvoiceChange.invoice_payment_change(
                new InvoicePaymentChange(
                        "paymentId",
                        InvoicePaymentChangePayload.invoice_payment_refund_change(
                                new InvoicePaymentRefundChange()
                                        .setId("refundId"))));

        InvoiceChangeWithMetadata invoiceChangeWithMetadata = InvoiceChangeWithMetadata.builder()
                .invoiceId("invoiceId")
                .eventId(0L)
                .invoiceChange(invoiceChange)
                .build();

        Invoice aggregate = new Invoice()
                .setInvoice(new com.rbkmoney.damsel.domain.Invoice())
                .setPayments(List.of(new InvoicePayment()
                        .setRefunds(List.of(new InvoicePaymentRefund()
                                .setRefund(new com.rbkmoney.damsel.domain.InvoicePaymentRefund()
                                        .setId("refundId"))))));

        when(invoiceAggregateRepository.findByInvoiceIdAndEventId("invoiceId", 0L))
                .thenReturn(aggregate);

        // When
        handler.handle(invoiceChangeWithMetadata);

        // Then
        verify(receiptFactory, times(1))
                .refundDebitForInvoice(any(), any());
        verify(cashregService, times(1))
                .send(any());
    }

    @Test
    public void shouldSendRefundDebitForPreviousPartialRefund() {
        // Given
        InvoiceChange invoiceChange = InvoiceChange.invoice_payment_change(
                new InvoicePaymentChange(
                        "paymentId",
                        InvoicePaymentChangePayload.invoice_payment_refund_change(
                                new InvoicePaymentRefundChange()
                                        .setId("refundId"))));

        InvoiceChangeWithMetadata invoiceChangeWithMetadata = InvoiceChangeWithMetadata.builder()
                .invoiceId("invoiceId")
                .eventId(0L)
                .invoiceChange(invoiceChange)
                .build();

        Invoice aggregate = new Invoice()
                .setInvoice(new com.rbkmoney.damsel.domain.Invoice())
                .setPayments(List.of(new InvoicePayment()
                        .setRefunds(List.of(
                                new InvoicePaymentRefund()
                                        .setRefund(new com.rbkmoney.damsel.domain.InvoicePaymentRefund()
                                                .setId("previousRefundId")
                                                .setStatus(InvoicePaymentRefundStatus.succeeded(
                                                        new InvoicePaymentRefundSucceeded()))
                                                .setCart(new InvoiceCart())),
                                new InvoicePaymentRefund()
                                        .setRefund(new com.rbkmoney.damsel.domain.InvoicePaymentRefund()
                                                .setId("refundId"))))));

        when(receiptFactory.refundDebitForPreviousPartialRefund(any(), any(), any()))
                .thenReturn(new ReceiptParams());
        when(invoiceAggregateRepository.findByInvoiceIdAndEventId("invoiceId", 0L))
                .thenReturn(aggregate);

        // When
        handler.handle(invoiceChangeWithMetadata);

        // Then
        verify(receiptFactory, times(1))
                .refundDebitForPreviousPartialRefund(any(), any(), any());
        verify(cashregService, times(1))
                .send(any());
    }

    @Test
    public void shouldSendRefundDebitForInvoiceAndDebitForPartialRefund() {
        // Given
        InvoiceChange invoiceChange = InvoiceChange.invoice_payment_change(
                new InvoicePaymentChange(
                        "paymentId",
                        InvoicePaymentChangePayload.invoice_payment_refund_change(
                                new InvoicePaymentRefundChange()
                                        .setId("refundId"))));

        InvoiceChangeWithMetadata invoiceChangeWithMetadata = InvoiceChangeWithMetadata.builder()
                .invoiceId("invoiceId")
                .eventId(0L)
                .invoiceChange(invoiceChange)
                .build();

        Invoice aggregate = new Invoice()
                .setInvoice(new com.rbkmoney.damsel.domain.Invoice())
                .setPayments(List.of(new InvoicePayment()
                        .setRefunds(List.of(new InvoicePaymentRefund()
                                .setRefund(new com.rbkmoney.damsel.domain.InvoicePaymentRefund()
                                        .setId("refundId")
                                        .setCart(new InvoiceCart()))))));

        when(invoiceAggregateRepository.findByInvoiceIdAndEventId("invoiceId", 0L))
                .thenReturn(aggregate);

        // When
        handler.handle(invoiceChangeWithMetadata);

        // Then
        verify(receiptFactory, times(1))
                .refundDebitForInvoice(any(), any());
        verify(receiptFactory, times(1))
                .debitForPartialRefund(any(), any(), any());
        verify(cashregService, times(2))
                .send(any());
    }
}