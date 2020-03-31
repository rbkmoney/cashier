package com.rbkmoney.cashier.service;

import com.rbkmoney.damsel.cashreg.ItemsLine;
import com.rbkmoney.damsel.cashreg.type.Debit;
import com.rbkmoney.damsel.cashreg.type.RefundDebit;
import com.rbkmoney.damsel.cashreg.type.Type;
import com.rbkmoney.damsel.cashreg_processing.CashRegParams;
import com.rbkmoney.damsel.cashreg_processing.ManagementSrv;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class CashRegServiceTest {

    @Mock
    private CartTransformer cartTransformer;
    @Mock
    private EmailExtractor emailExtractor;
    @Mock
    private ManagementSrv.Iface cashRegClient;

    @InjectMocks
    private CashRegService cashRegService;

    @Before
    public void setUp() {
        cashRegService.setReceiptsSendingEnabled(true);

        when(cartTransformer.transform(any()))
                .thenReturn(List.of(new ItemsLine()));
        when(emailExtractor.extract(any()))
                .thenReturn("email");
    }

    @Test
    public void shouldSendReceipts() throws TException {
        // Given - When
        cashRegService.send(
                new CashRegParams(),
                new CashRegParams(),
                new CashRegParams());

        // Then
        verify(cashRegClient, times(3))
                .create(any());
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
        CashRegParams receipt = cashRegService.debitForInvoice(
                "providerId",
                aggregate);

        // Then
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
        CashRegParams receipt = cashRegService.debitForPartialCapture(
                "providerId",
                aggregate,
                capturedPayment);

        // Then
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
        CashRegParams receipt = cashRegService.debitForPartialRefund(
                "providerId",
                aggregate,
                refund);

        // Then
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
        CashRegParams receipt = cashRegService.refundDebitForInvoice(
                "providerId",
                aggregate);

        // Then
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
        CashRegParams receipt = cashRegService.refundDebitForPreviousPartialRefund(
                "providerId",
                aggregate,
                refund);

        // Then
        assertThat(receipt.getPartyId()).isEqualTo("ownerId");
        assertThat(receipt.getShopId()).isEqualTo("shopId");
        assertThat(receipt.getType()).isEqualTo(Type.refund_debit(new RefundDebit()));
        assertThat(receipt.getPaymentInfo().getCart().getLines()).hasSize(1);
        assertThat(receipt.getPaymentInfo().getEmail()).isEqualTo("email");
        assertThat(receipt.getPaymentInfo().getCash().getAmount()).isEqualTo(80L);
        assertThat(receipt.getPaymentInfo().getCash().getCurrency().getSymbolicCode()).isEqualTo("RUB");
    }
}