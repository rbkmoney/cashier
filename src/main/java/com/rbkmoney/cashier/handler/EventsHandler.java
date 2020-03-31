package com.rbkmoney.cashier.handler;

import com.rbkmoney.cashier.domain.InvoiceChangeWithMetadata;
import com.rbkmoney.cashier.handler.inventory.HandlerInventory;
import com.rbkmoney.cashier.parser.SourceEventParser;
import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

import static java.util.Comparator.comparingLong;
import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class EventsHandler {

    private final HandlerInventory handlerInventory;
    private final SourceEventParser eventParser;

    public void handle(List<MachineEvent> machineEvents) {
        machineEvents.stream()
                .map(machineEvent -> Map.entry(eventParser.parse(machineEvent), machineEvent))
                .filter(entry -> entry.getKey().isSetInvoiceChanges())
                .map(this::toInvoiceChangesWithMetadata)
                .flatMap(List::stream)
                .sorted(byEventId())
                .forEach(invoiceChangeWithMetadata -> handlerInventory
                        .getFor(invoiceChangeWithMetadata.getInvoiceChange())
                        .handle(invoiceChangeWithMetadata));
    }

    private List<InvoiceChangeWithMetadata> toInvoiceChangesWithMetadata(
            Map.Entry<EventPayload, MachineEvent> payloadWithMachineEvent) {
        MachineEvent machineEvent = payloadWithMachineEvent.getValue();
        String invoiceId = machineEvent.getSourceId();
        long eventId = machineEvent.getEventId();

        return payloadWithMachineEvent.getKey().getInvoiceChanges()
                .stream()
                .map(change -> InvoiceChangeWithMetadata.builder()
                        .invoiceId(invoiceId)
                        .eventId(eventId)
                        .invoiceChange(change)
                        .build())
                .collect(toList());
    }

    private Comparator<InvoiceChangeWithMetadata> byEventId() {
        return comparingLong(InvoiceChangeWithMetadata::getEventId);
    }
}
