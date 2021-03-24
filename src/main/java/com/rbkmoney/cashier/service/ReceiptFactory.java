package com.rbkmoney.cashier.service;

import com.rbkmoney.cashier.domain.CashRegister;
import com.rbkmoney.cashier.mapper.CashRegisterMapper;
import com.rbkmoney.damsel.cashreg.domain.PaymentInfo;
import com.rbkmoney.damsel.cashreg.processing.ReceiptParams;
import com.rbkmoney.damsel.cashreg.receipt.Cart;
import com.rbkmoney.damsel.cashreg.receipt.ItemsLine;
import com.rbkmoney.damsel.cashreg.receipt.type.Debit;
import com.rbkmoney.damsel.cashreg.receipt.type.RefundDebit;
import com.rbkmoney.damsel.cashreg.receipt.type.Type;
import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.domain.CurrencyRef;
import com.rbkmoney.damsel.domain.InvoicePaymentCaptured;
import com.rbkmoney.damsel.domain.InvoicePaymentRefund;
import com.rbkmoney.damsel.payment_processing.Invoice;
import com.rbkmoney.damsel.payment_processing.InvoicePayment;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReceiptFactory {

    private static final String DEBIT = "debit";
    private static final String CREDIT = "credit";
    private static final String REFUND_DEBIT = "refund-debit";
    private static final String REFUND_CREDIT = "refund-credit";

    private final CartTransformer cartTransformer;
    private final EmailExtractor emailExtractor;
    private final CashRegisterMapper cashRegisterMapper;

    public ReceiptParams debitForInvoice(
            List<CashRegister> cashRegisters,
            Invoice aggregate,
            long eventId) {
        log.debug("Creating new DEBIT receipt for invoice...");

        Cash cash = aggregate.getInvoice().getCost();
        List<ItemsLine> items = cartTransformer.transform(aggregate.getInvoice().getDetails().getCart());

        return receipt(
                cashRegisters,
                aggregate,
                eventId,
                Type.debit(new Debit()),
                cash,
                items);
    }

    public ReceiptParams debitForPartialCapture(
            List<CashRegister> cashRegisters,
            Invoice aggregate,
            long eventId,
            InvoicePaymentCaptured capturedPayment) {
        log.debug("Creating new DEBIT receipt for partial capture...");

        Cash cash = capturedPayment.getCost();
        List<ItemsLine> items = cartTransformer.transform(capturedPayment.getCart());

        return receipt(
                cashRegisters,
                aggregate,
                eventId,
                Type.debit(new Debit()),
                cash,
                items);
    }

    public ReceiptParams debitForPartialRefund(
            List<CashRegister> cashRegisters,
            Invoice aggregate,
            long eventId,
            InvoicePaymentRefund refund) {
        log.debug("Creating new DEBIT receipt for partial refund...");

        Cash cash = cashForPartialRefund(aggregate, refund);
        List<ItemsLine> items = cartTransformer.transform(refund.getCart());

        return receipt(
                cashRegisters,
                aggregate,
                eventId,
                Type.debit(new Debit()),
                cash,
                items);
    }

    public ReceiptParams refundDebitForInvoice(
            List<CashRegister> cashRegisters,
            Invoice aggregate,
            long eventId) {
        log.debug("Creating new REFUND_DEBIT receipt for invoice...");

        Cash cash = aggregate.getInvoice().getCost();
        List<ItemsLine> items = cartTransformer.transform(aggregate.getInvoice().getDetails().getCart());

        return receipt(
                cashRegisters,
                aggregate,
                eventId,
                Type.refund_debit(new RefundDebit()),
                cash,
                items);
    }

    public ReceiptParams refundDebitForPreviousPartialRefund(
            List<CashRegister> cashRegisters,
            Invoice aggregate,
            long eventId,
            InvoicePaymentRefund refund) {
        log.debug("Creating new REFUND_DEBIT receipt for previous partial refund...");

        Cash cash = cashForPartialRefund(aggregate, refund);
        List<ItemsLine> items = cartTransformer.transform(refund.getCart());

        return receipt(
                cashRegisters,
                aggregate,
                eventId,
                Type.refund_debit(new RefundDebit()),
                cash,
                items);
    }

    private ReceiptParams receipt(
            List<CashRegister> cashRegisters,
            Invoice aggregate,
            long eventId,
            Type type,
            Cash cash,
            List<ItemsLine> items) {
        com.rbkmoney.damsel.domain.Invoice invoice = aggregate.getInvoice();
        List<InvoicePayment> payments = aggregate.getPayments();

        String id = receiptId(invoice.getId(), eventId, type);

        String partyId = invoice.getOwnerId();
        String shopId = invoice.getShopId();
        String email = emailExtractor.extract(payments);

        ReceiptParams receipt = new ReceiptParams()
                .setReceiptId(id)
                .setPartyId(partyId)
                .setShopId(shopId)
                .setProviders(cashRegisterMapper.map(cashRegisters))
                .setType(type)
                .setPaymentInfo(
                        new PaymentInfo()
                                .setEmail(email)
                                .setCash(cash)
                                .setCart(new Cart(items)));

        log.info("New receipt created={}", receipt);
        return receipt;
    }

    private String receiptId(
            String invoiceId,
            long eventId,
            Type type) {
        return String.format("%s.%s.%s",
                invoiceId,
                eventId,
                typeString(type));
    }

    private String typeString(Type type) {
        if (type.isSetCredit()) {
            return CREDIT;
        }

        if (type.isSetDebit()) {
            return DEBIT;
        }

        if (type.isSetRefundCredit()) {
            return REFUND_CREDIT;
        }

        if (type.isSetRefundDebit()) {
            return REFUND_DEBIT;
        }

        throw new IllegalArgumentException("Unknown receipt type: " + type);
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
                .mapToLong(item -> item.getPrice().getAmount())
                .sum();

        return new Cash(amount, currency);
    }
}
