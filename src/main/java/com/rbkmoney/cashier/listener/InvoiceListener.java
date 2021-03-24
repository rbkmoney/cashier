package com.rbkmoney.cashier.listener;

import com.rbkmoney.cashier.handler.EventsHandler;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.stereotype.Service;

import java.util.List;

import static java.util.stream.Collectors.toList;

@Slf4j
@Service
@RequiredArgsConstructor
public class InvoiceListener {

    private final EventsHandler eventsHandler;

    @KafkaListener(
            autoStartup = "${kafka.auto-startup}",
            topics = "${kafka.invoice-topic}",
            containerFactory = "kafkaListenerContainerFactory")
    public void listen(
            List<SinkEvent> batch,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) int offset,
            Acknowledgment ack) {
        log.info("Listening Invoice: partition={}, offset={}, batch.size()={}", partition, offset, batch.size());
        List<MachineEvent> machineEvents = batch.stream()
                .map(SinkEvent::getEvent)
                .collect(toList());

        eventsHandler.handle(machineEvents);

        ack.acknowledge();
        log.info("Ack Invoice: partition={}, offset={}", partition, offset);
    }
}
