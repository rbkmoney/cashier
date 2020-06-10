package com.rbkmoney.cashier;

import com.rbkmoney.cashier.domain.CashRegister;
import com.rbkmoney.cashier.handler.EventsHandler;
import com.rbkmoney.cashier.repository.CashRegisterRepository;
import com.rbkmoney.cashier.repository.InvoiceAggregateRepository;
import com.rbkmoney.cashier.util.JsonMapper;
import com.rbkmoney.damsel.cashreg.processing.ManagementSrv;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.payment_processing.*;
import com.rbkmoney.damsel.payment_processing.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import com.rbkmoney.damsel.payment_processing.InvoicePaymentRefund;
import com.rbkmoney.geck.serializer.kit.mock.MockMode;
import com.rbkmoney.geck.serializer.kit.mock.MockTBaseProcessor;
import com.rbkmoney.geck.serializer.kit.tbase.TBaseHandler;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.SneakyThrows;
import org.apache.thrift.TBase;
import org.apache.thrift.TException;
import org.apache.thrift.TSerializer;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.jetbrains.annotations.NotNull;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = CashierApplication.class)
@ContextConfiguration(initializers = IntegrationTestWithNoKafka.Initializer.class)
@EnableAutoConfiguration(exclude = DataSourceAutoConfiguration.class)
public class IntegrationTestWithNoKafka {

    @Autowired
    private EventsHandler eventsHandler;

    @MockBean
    private InvoiceAggregateRepository invoiceAggregateRepository;

    @MockBean
    private CashRegisterRepository cashRegisterRepository;

    @MockBean
    private ManagementSrv.Iface cashregClient;

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {

        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "kafka.auto-startup=false",
                    "spring.flyway.enabled=false")
                    .applyTo(configurableApplicationContext.getEnvironment());
        }
    }

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
                                .setRefund(new com.rbkmoney.damsel.domain.InvoicePaymentRefund()
                                        .setId("refundId")
                                        .setCart(new InvoiceCart(List.of())))))));

        when(invoiceAggregateRepository.findByInvoiceIdAndEventId("invoiceId", 0L))
                .thenReturn(aggregate);

        List<CashRegister> cashRegisters = prepareListCashRegisters();
        when(cashRegisterRepository.findByPartyIdAndShopId(anyString(), anyString()))
                .thenReturn(cashRegisters);
    }

    @Test
    public void shouldSendReceiptsToCashReg() throws TException {
        eventsHandler.handle(List.of(machineEvent()));

        verify(cashregClient, times(7))
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

        return new MachineEvent()
                .setCreatedAt(Instant.now().toString())
                .setEventId(0L)
                .setSourceNs("sourceNs")
                .setSourceId("invoiceId")
                .setData(com.rbkmoney.machinegun.msgpack.Value.bin(toByteArray(eventPayload)));
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


    @NotNull
    private List<CashRegister> prepareListCashRegisters() {
        List<CashRegister> cashRegisters = new ArrayList<>();
        CashRegister cashRegister = CashRegister.builder()
                .id("id")
                .partyId("partyId")
                .shopId("shopId")
                .providerId(1)
                .providerParams(JsonMapper.toJson(prepareProviderParams()))
                .build();
        cashRegisters.add(cashRegister);
        return cashRegisters;
    }

    @NotNull
    private Map<String, String> prepareProviderParams() {
        return Map.of(
                "name", "pupa",
                "password", "lupa",
                "tel", "88005553535",
                "url", "https://pupalupa.com/");
    }
}