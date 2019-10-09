package com.rbkmoney.cashier.service;

import com.rbkmoney.damsel.cashreg.Cart;
import com.rbkmoney.damsel.cashreg.ItemsLine;
import com.rbkmoney.damsel.cashreg.type.Debit;
import com.rbkmoney.damsel.cashreg.type.RefundDebit;
import com.rbkmoney.damsel.cashreg.type.Type;
import com.rbkmoney.damsel.cashreg_domain.PaymentInfo;
import com.rbkmoney.damsel.cashreg_processing.CashRegParams;
import com.rbkmoney.damsel.cashreg_processing.ManagementSrv;
import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.domain.CurrencyRef;
import com.rbkmoney.damsel.domain.InvoicePaymentCaptured;
import com.rbkmoney.damsel.domain.InvoicePaymentRefund;
import com.rbkmoney.damsel.payment_processing.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashRegService {

    @Setter
    @Value("${client.cash-reg.enabled}")
    private boolean isReceiptsSendingEnabled;

    private final CartTransformer cartTransformer;
    private final EmailExtractor emailExtractor;
    private final ManagementSrv.Iface cashRegClient;

    public void send(CashRegParams... receipts) {
        if (!isReceiptsSendingEnabled) {
            log.debug("Sending receipts to cashReg is disabled!");
            return;
        }

        for (CashRegParams receipt : receipts) {
            try {
                log.debug("Sending receipt={} to cashReg...", receipt);
                cashRegClient.create(receipt);
            } catch (TException e) {
                log.error("CashRegClient exception for receipt={}: ", receipt, e);
            }
        }
    }

    public CashRegParams debitForInvoice(Invoice aggregate) {
        log.debug("Creating new DEBIT receipt for invoice...");

        Cash cash = aggregate.getInvoice().getCost();
        List<ItemsLine> items = cartTransformer.transform(aggregate.getInvoice().getDetails().getCart());

        return receipt(
                aggregate,
                Type.debit(new Debit()),
                cash,
                items);
    }

    public CashRegParams debitForPartialCapture(
            Invoice aggregate,
            InvoicePaymentCaptured capturedPayment) {
        log.debug("Creating new DEBIT receipt for partial capture...");

        Cash cash = capturedPayment.getCost();
        List<ItemsLine> items = cartTransformer.transform(capturedPayment.getCart());

        return receipt(
                aggregate,
                Type.debit(new Debit()),
                cash,
                items);
    }

    public CashRegParams debitForPartialRefund(
            Invoice aggregate,
            InvoicePaymentRefund refund) {
        log.debug("Creating new DEBIT receipt for partial refund...");

        Cash cash = cashForPartialRefund(aggregate, refund);
        List<ItemsLine> items = cartTransformer.transform(refund.getCart());

        return receipt(
                aggregate,
                Type.debit(new Debit()),
                cash,
                items);
    }

    public CashRegParams refundDebitForInvoice(Invoice aggregate) {
        log.debug("Creating new REFUND_DEBIT receipt for invoice...");

        Cash cash = aggregate.getInvoice().getCost();
        List<ItemsLine> items = cartTransformer.transform(aggregate.getInvoice().getDetails().getCart());

        return receipt(
                aggregate,
                Type.refund_debit(new RefundDebit()),
                cash,
                items);
    }

    public CashRegParams refundDebitForPreviousPartialRefund(
            Invoice aggregate,
            InvoicePaymentRefund refund) {
        log.debug("Creating new REFUND_DEBIT receipt for previous partial refund...");

        Cash cash = cashForPartialRefund(aggregate, refund);
        List<ItemsLine> items = cartTransformer.transform(refund.getCart());

        return receipt(
                aggregate,
                Type.refund_debit(new RefundDebit()),
                cash,
                items);
    }

    private CashRegParams receipt(
            Invoice aggregate,
            Type type,
            Cash cash,
            List<ItemsLine> items) {
        com.rbkmoney.damsel.domain.Invoice invoice = aggregate.getInvoice();
        List<InvoicePayment> payments = aggregate.getPayments();

        String id = receiptId(invoice, payments);

        String partyId = invoice.getOwnerId();
        String shopId = invoice.getShopId();
        String email = emailExtractor.extract(payments);

        CashRegParams receipt = new CashRegParams()
                .setId(id)
                .setPartyId(partyId)
                .setShopId(shopId)
                .setType(type)
                .setPaymentInfo(
                        new PaymentInfo()
                                .setEmail(email)
                                .setCash(cash)
                                .setCart(new Cart(items)));

        log.debug("New receipt created={}", receipt);
        return receipt;
    }

    private String receiptId(
            com.rbkmoney.damsel.domain.Invoice invoice,
            List<InvoicePayment> payments) {
        return String.format("%s.%s.%s",
                invoice.getId(),
                payments.get(payments.size() - 1).getPayment().getId(),
                UUID.randomUUID());
    }

    private Cash cashForPartialRefund(
            Invoice aggregate,
            InvoicePaymentRefund refund) {
        CurrencyRef currency = aggregate
                .getInvoice()
                .getCost()
                .getCurrency();

        long amount = refund
                .getCart()
                .getLines()
                .stream()
                .mapToLong(line -> line.getPrice().getAmount())
                .sum();

        return new Cash(amount, currency);
    }
}
