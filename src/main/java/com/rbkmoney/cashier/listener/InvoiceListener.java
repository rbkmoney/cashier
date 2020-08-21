package com.rbkmoney.cashier.listener;

import com.rbkmoney.cashier.handler.EventsHandler;
import com.rbkmoney.kafka.common.util.LogUtil;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
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
            List<ConsumerRecord<String, MachineEvent>> messages,
            Acknowledgment ack) {
        List<MachineEvent> machineEvents = messages.stream()
                .map(ConsumerRecord::value)
                .collect(toList());

        eventsHandler.handle(machineEvents);
        ack.acknowledge();


        log.debug("{} records have been committed: {}",
                messages.size(),
                LogUtil.toSummaryStringWithMachineEventValues(messages));
    }
}
