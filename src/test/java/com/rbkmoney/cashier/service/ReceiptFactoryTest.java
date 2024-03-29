package com.rbkmoney.cashier.service;

import com.rbkmoney.cashier.mapper.CashRegisterMapper;
import com.rbkmoney.damsel.cashreg.processing.ReceiptParams;
import com.rbkmoney.damsel.cashreg.receipt.ItemsLine;
import com.rbkmoney.damsel.cashreg.receipt.type.Debit;
import com.rbkmoney.damsel.cashreg.receipt.type.RefundDebit;
import com.rbkmoney.damsel.cashreg.receipt.type.Type;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ReceiptFactoryTest {

    @Mock
    private CartTransformer cartTransformer;
    @Mock
    private EmailExtractor emailExtractor;
    @Mock
    private CashRegisterMapper cashRegisterMapper;

    @InjectMocks
    private ReceiptFactory receiptFactory;

    @Before
    public void setUp() {
        when(cartTransformer.transform(any()))
                .thenReturn(List.of(new ItemsLine()));
        when(emailExtractor.extract(any()))
                .thenReturn("email");
    }
    
    @Test
    public void shouldCreateDebitReceiptForInvoice() {
        // Given
        Invoice aggregate = new Invoice()
                .setInvoice(new com.rbkmoney.damsel.domain.Invoice()
                        .setId("invoiceId")
                        .setOwnerId("ownerId")
                        .setShopId("shopId")
                        .setDetails(new InvoiceDetails()
                                .setCart(new InvoiceCart()))
                        .setCost(new Cash()
                                .setAmount(100L)
                                .setCurrency(new CurrencyRef()
                                        .setSymbolicCode("RUB"))))
                .setPayments(List.of(new InvoicePayment()
                        .setPayment(new com.rbkmoney.damsel.domain.InvoicePayment()
                                .setId("paymentId"))));

        // When
        ReceiptParams receipt = receiptFactory.debitForInvoice(
                List.of(),
                aggregate,
                1L);

        // Then
        assertThat(receipt.getReceiptId()).isEqualTo("invoiceId.1.debit");
        assertThat(receipt.getPartyId()).isEqualTo("ownerId");
        assertThat(receipt.getShopId()).isEqualTo("shopId");
        assertThat(receipt.getType()).isEqualTo(Type.debit(new Debit()));
        assertThat(receipt.getPaymentInfo().getCart().getLines()).hasSize(1);
        assertThat(receipt.getPaymentInfo().getEmail()).isEqualTo("email");
        assertThat(receipt.getPaymentInfo().getCash().getAmount()).isEqualTo(100L);
        assertThat(receipt.getPaymentInfo().getCash().getCurrency().getSymbolicCode()).isEqualTo("RUB");
    }

    @Test
    public void shouldCreateDebitReceiptForPartialCapture() {
        // Given
        Invoice aggregate = new Invoice()
                .setInvoice(new com.rbkmoney.damsel.domain.Invoice()
                        .setId("invoiceId")
                        .setOwnerId("ownerId")
                        .setShopId("shopId")
                        .setDetails(new InvoiceDetails()
                                .setCart(new InvoiceCart()))
                        .setCost(new Cash()
                                .setAmount(100L)
                                .setCurrency(new CurrencyRef()
                                        .setSymbolicCode("RUB"))))
                .setPayments(List.of(new InvoicePayment()
                        .setPayment(new com.rbkmoney.damsel.domain.InvoicePayment()
                                .setId("paymentId"))));

        InvoicePaymentCaptured capturedPayment = new InvoicePaymentCaptured()
                .setCart(new InvoiceCart())
                .setCost(new Cash()
                        .setAmount(20L)
                        .setCurrency(new CurrencyRef()
                                .setSymbolicCode("RUB")));

        // When
        ReceiptParams receipt = receiptFactory.debitForPartialCapture(
                List.of(),
                aggregate,
                1L,
                capturedPayment);

        // Then
        assertThat(receipt.getReceiptId()).isEqualTo("invoiceId.1.debit");
        assertThat(receipt.getPartyId()).isEqualTo("ownerId");
        assertThat(receipt.getShopId()).isEqualTo("shopId");
        assertThat(receipt.getType()).isEqualTo(Type.debit(new Debit()));
        assertThat(receipt.getPaymentInfo().getCart().getLines()).hasSize(1);
        assertThat(receipt.getPaymentInfo().getEmail()).isEqualTo("email");
        assertThat(receipt.getPaymentInfo().getCash().getAmount()).isEqualTo(20L);
        assertThat(receipt.getPaymentInfo().getCash().getCurrency().getSymbolicCode()).isEqualTo("RUB");
    }

    @Test
    public void shouldCreateDebitReceiptForPartialRefund() {
        // Given
        Invoice aggregate = new Invoice()
                .setInvoice(new com.rbkmoney.damsel.domain.Invoice()
                        .setId("invoiceId")
                        .setOwnerId("ownerId")
                        .setShopId("shopId")
                        .setDetails(new InvoiceDetails()
                                .setCart(new InvoiceCart()))
                        .setCost(new Cash()
                                .setAmount(100L)
                                .setCurrency(new CurrencyRef()
                                        .setSymbolicCode("RUB"))))
                .setPayments(List.of(new InvoicePayment()
                        .setPayment(new com.rbkmoney.damsel.domain.InvoicePayment()
                                .setId("paymentId"))));

        InvoicePaymentRefund refund = new InvoicePaymentRefund()
                .setCart(new InvoiceCart()
                        .setLines(List.of(new InvoiceLine()
                                .setPrice(new Cash()
                                        .setAmount(80L)
                                        .setCurrency(new CurrencyRef()
                                                .setSymbolicCode("RUB"))))))
                .setCash(new Cash()
                        .setAmount(80L)
                        .setCurrency(new CurrencyRef()
                                .setSymbolicCode("RUB")));

        // When
        ReceiptParams receipt = receiptFactory.debitForPartialRefund(
                List.of(),
                aggregate,
                1L,
                refund);

        // Then
        assertThat(receipt.getReceiptId()).isEqualTo("invoiceId.1.debit");
        assertThat(receipt.getPartyId()).isEqualTo("ownerId");
        assertThat(receipt.getShopId()).isEqualTo("shopId");
        assertThat(receipt.getType()).isEqualTo(Type.debit(new Debit()));
        assertThat(receipt.getPaymentInfo().getCart().getLines()).hasSize(1);
        assertThat(receipt.getPaymentInfo().getEmail()).isEqualTo("email");
        assertThat(receipt.getPaymentInfo().getCash().getAmount()).isEqualTo(80L);
        assertThat(receipt.getPaymentInfo().getCash().getCurrency().getSymbolicCode()).isEqualTo("RUB");
    }

    @Test
    public void shouldCreateRefundDebitReceiptForInvoice() {
        // Given
        Invoice aggregate = new Invoice()
                .setInvoice(new com.rbkmoney.damsel.domain.Invoice()
                        .setId("invoiceId")
                        .setOwnerId("ownerId")
                        .setShopId("shopId")
                        .setDetails(new InvoiceDetails()
                                .setCart(new InvoiceCart()))
                        .setCost(new Cash()
                                .setAmount(100L)
                                .setCurrency(new CurrencyRef()
                                        .setSymbolicCode("RUB"))))
                .setPayments(List.of(new InvoicePayment()
                        .setPayment(new com.rbkmoney.damsel.domain.InvoicePayment()
                                .setId("paymentId"))));

        // When
        ReceiptParams receipt = receiptFactory.refundDebitForInvoice(
                List.of(),
                aggregate,
                1L);

        // Then
        assertThat(receipt.getReceiptId()).isEqualTo("invoiceId.1.refund-debit");
        assertThat(receipt.getPartyId()).isEqualTo("ownerId");
        assertThat(receipt.getShopId()).isEqualTo("shopId");
        assertThat(receipt.getType()).isEqualTo(Type.refund_debit(new RefundDebit()));
        assertThat(receipt.getPaymentInfo().getCart().getLines()).hasSize(1);
        assertThat(receipt.getPaymentInfo().getEmail()).isEqualTo("email");
        assertThat(receipt.getPaymentInfo().getCash().getAmount()).isEqualTo(100L);
        assertThat(receipt.getPaymentInfo().getCash().getCurrency().getSymbolicCode()).isEqualTo("RUB");
    }

    @Test
    public void shouldCreateRefundDebitReceiptForPreviousPartialRefund() {
        // Given
        Invoice aggregate = new Invoice()
                .setInvoice(new com.rbkmoney.damsel.domain.Invoice()
                        .setId("invoiceId")
                        .setOwnerId("ownerId")
                        .setShopId("shopId")
                        .setDetails(new InvoiceDetails()
                                .setCart(new InvoiceCart()))
                        .setCost(new Cash()
                                .setAmount(100L)
                                .setCurrency(new CurrencyRef()
                                        .setSymbolicCode("RUB"))))
                .setPayments(List.of(new InvoicePayment()
                        .setPayment(new com.rbkmoney.damsel.domain.InvoicePayment()
                                .setId("paymentId"))));

        InvoicePaymentRefund refund = new InvoicePaymentRefund()
                .setCart(new InvoiceCart()
                        .setLines(List.of(new InvoiceLine()
                                .setPrice(new Cash()
                                        .setAmount(80L)
                                        .setCurrency(new CurrencyRef()
                                                .setSymbolicCode("RUB"))))))
                .setCash(new Cash()
                        .setAmount(80L)
                        .setCurrency(new CurrencyRef()
                                .setSymbolicCode("RUB")));

        // When
        ReceiptParams receipt = receiptFactory.refundDebitForPreviousPartialRefund(
                List.of(),
                aggregate,
                1L,
                refund);

        // Then
        assertThat(receipt.getReceiptId()).isEqualTo("invoiceId.1.refund-debit");
        assertThat(receipt.getPartyId()).isEqualTo("ownerId");
        assertThat(receipt.getShopId()).isEqualTo("shopId");
        assertThat(receipt.getType()).isEqualTo(Type.refund_debit(new RefundDebit()));
        assertThat(receipt.getPaymentInfo().getCart().getLines()).hasSize(1);
        assertThat(receipt.getPaymentInfo().getEmail()).isEqualTo("email");
        assertThat(receipt.getPaymentInfo().getCash().getAmount()).isEqualTo(80L);
        assertThat(receipt.getPaymentInfo().getCash().getCurrency().getSymbolicCode()).isEqualTo("RUB");
    }
}