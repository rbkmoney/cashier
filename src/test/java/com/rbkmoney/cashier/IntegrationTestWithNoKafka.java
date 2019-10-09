package com.rbkmoney.cashier;

import com.rbkmoney.cashier.handler.EventsHandler;
import com.rbkmoney.cashier.repository.InvoiceAggregateRepository;
import com.rbkmoney.damsel.cashreg_processing.ManagementSrv;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.geck.serializer.kit.mock.MockMode;
import com.rbkmoney.geck.serializer.kit.mock.MockTBaseProcessor;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseHandler;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.SneakyThrows;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.time.Instant;
import java.util.List;

import static org.mockito.Mockito.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {CashierApplication.class})
@TestPropertySource(properties = {
        "spring.application.name=cashier",
        "kafka.auto-startup=false",
        "kafka.bootstrap-servers=localhost:9091",
        "kafka.client-id=cashier",
        "kafka.invoice-topic=mg-invoice-100-2",
        "kafka.consumer.group-id=cashier-group-1",
        "kafka.consumer.enable-auto-commit=false",
        "kafka.consumer.auto-offset-reset=latest",
        "kafka.consumer.max-poll-records=500",
        "kafka.consumer.concurrency=1",
        "kafka.concurrency=5",
        "kafka.ssl.enabled=false",
        "client.cash-reg.url=http://cashreg:8080",
        "client.cash-reg.timeout=5000",
        "client.cash-reg.enabled=true",
        "events.path.payment-processed=invoice_payment_change.payload.invoice_payment_status_changed.status.processed",
        "events.path.payment-captured=invoice_payment_change.payload.invoice_payment_status_changed.status.captured",
        "events.path.payment-cancelled=invoice_payment_change.payload.invoice_payment_status_changed.status.cancelled",
        "events.path.payment-failed=invoice_payment_change.payload.invoice_payment_status_changed.status.failed",
        "events.path.refund-succeeded=invoice_payment_change.payload.invoice_payment_refund_change.payload.invoice_payment_refund_status_changed.status.succeeded"
})
public class IntegrationTestWithNoKafka {

    @Autowired
    private EventsHandler eventsHandler;

    @MockBean
    private InvoiceAggregateRepository repository;

    @MockBean
    private ManagementSrv.Iface cashRegClient;

    @Before
    public void setUp() {
        Invoice aggregate = new Invoice()
                .setInvoice(new com.rbkmoney.damsel.domain.Invoice()
                        .setId("invoiceId")
                        .setOwnerId("ownerId")
                        .setShopId("shopId")
                        .setDetails(new InvoiceDetails()
                                .setCart(new InvoiceCart(List.of())))
                        .setCost(new Cash()
                                .setAmount(100L)
                                .setCurrency(new CurrencyRef()
                                        .setSymbolicCode("RUB"))))
                .setPayments(List.of(new InvoicePayment()
                        .setPayment(new com.rbkmoney.damsel.domain.InvoicePayment()
                                .setId("paymentId"))
                        .setRefunds(List.of(new InvoicePaymentRefund()
                                .setId("refundId")
                                .setCart(new InvoiceCart(List.of()))))));

        when(repository.findByInvoiceIdAndEventId("invoiceId", 0L))
                .thenReturn(aggregate);
    }

    @Test
    public void shouldSendReceiptsToCashReg() throws TException {
        eventsHandler.handle(List.of(machineEvent()));

        verify(cashRegClient, times(7))
                .create(any());
    }

    private MachineEvent machineEvent() {
        // 1 receipt: debit
        InvoiceChange paymentProcessed = fillTBaseObject(
                InvoiceChange.invoice_payment_change(
                        new InvoicePaymentChange(
                                "paymentId",
                                InvoicePaymentChangePayload.invoice_payment_status_changed(
                                        new InvoicePaymentStatusChanged(
                                                InvoicePaymentStatus.processed(
                                                        new InvoicePaymentProcessed()))))));

        // 2 receipts: debit and refund_debit
        InvoiceChange partialCapture = fillTBaseObject(
                InvoiceChange.invoice_payment_change(
                        new InvoicePaymentChange(
                                "paymentId",
                                InvoicePaymentChangePayload.invoice_payment_status_changed(
                                        new InvoicePaymentStatusChanged(
                                                InvoicePaymentStatus.captured(
                                                        new InvoicePaymentCaptured()
                                                                .setCart(new InvoiceCart())))))));

        // 1 receipt: refund_debit
        InvoiceChange paymentCancelled = fillTBaseObject(
                InvoiceChange.invoice_payment_change(
                        new InvoicePaymentChange(
                                "paymentId",
                                InvoicePaymentChangePayload.invoice_payment_status_changed(
                                        new InvoicePaymentStatusChanged(
                                                InvoicePaymentStatus.cancelled(
                                                        new InvoicePaymentCancelled()))))));

        // 1 receipt: refund_debit
        InvoiceChange paymentFailed = fillTBaseObject(
                InvoiceChange.invoice_payment_change(
                        new InvoicePaymentChange(
                                "paymentId",
                                InvoicePaymentChangePayload.invoice_payment_status_changed(
                                        new InvoicePaymentStatusChanged(
                                                InvoicePaymentStatus.failed(
                                                        new InvoicePaymentFailed()))))));

        // 2 receipts: debit and refund_debit
        InvoiceChange partialRefund = fillTBaseObject(
                InvoiceChange.invoice_payment_change(
                        new InvoicePaymentChange(
                                "paymentId",
                                InvoicePaymentChangePayload.invoice_payment_refund_change(
                                        new InvoicePaymentRefundChange(
                                                "refundId",
                                                InvoicePaymentRefundChangePayload.invoice_payment_refund_status_changed(
                                                        new InvoicePaymentRefundStatusChanged(
                                                                InvoicePaymentRefundStatus.succeeded(
                                                                        new InvoicePaymentRefundSucceeded()))))))));

        EventPayload eventPayload = EventPayload.invoice_changes(List.of(
                paymentProcessed,
                paymentCancelled,
                paymentFailed,
                partialCapture,
                partialRefund));

        MachineEvent event = new MachineEvent();
        event.setCreatedAt(Instant.now().toString());
        event.setEventId(0L);
        event.setSourceNs("sourceNs");
        event.setSourceId("invoiceId");
        event.setData(com.rbkmoney.machinegun.msgpack.Value.bin(toByteArray(eventPayload)));

        return event;
    }

    @SneakyThrows
    public byte[] toByteArray(TBase tBase) {
        return new TSerializer(new TBinaryProtocol.Factory()).serialize(tBase);
    }

    @SneakyThrows
    private static InvoiceChange fillTBaseObject(InvoiceChange tBase) {
        return new MockTBaseProcessor(MockMode.RANDOM, 15, 1)
                .process(tBase, new TBaseHandler<>(InvoiceChange.class));
    }
}