package com.rbkmoney.cashier.handler.inventory;

import com.rbkmoney.cashier.handler.events.iface.EventHandler;
import com.rbkmoney.damsel.payment_processing.InvoiceChange;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class HandlerInventory {

    private final List<EventHandler> handlers;

    public EventHandler getFor(InvoiceChange change) {
        return handlers.stream()
                .filter(handler -> handler.isApplicable(change))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("No handler was found for invoice change=" + change));

    }
}
