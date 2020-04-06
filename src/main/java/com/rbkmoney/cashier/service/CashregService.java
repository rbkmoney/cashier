package com.rbkmoney.cashier.service;

import com.rbkmoney.damsel.cashreg.processing.ManagementSrv;
import com.rbkmoney.damsel.cashreg.processing.ReceiptParams;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashregService {

    @Setter
    @Value("${client.cashreg.enabled}")
    private boolean isReceiptsSendingEnabled;

    private final ManagementSrv.Iface cashregClient;

    public void send(ReceiptParams... receipts) {
        if (!isReceiptsSendingEnabled) {
            log.debug("Sending receipts to cashreg is disabled!");
            return;
        }

        for (ReceiptParams receipt : receipts) {
            try {
                log.debug("Sending receipt={} to cashreg...", receipt);
                cashregClient.create(receipt);
            } catch (TException e) {
                log.error("CashregClient exception for receipt={}: ", receipt, e);
            }
        }
    }
}
