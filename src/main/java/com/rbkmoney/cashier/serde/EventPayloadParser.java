package com.rbkmoney.cashier.serde;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.sink.common.parser.impl.MachineEventParser;
import com.rbkmoney.sink.common.serialization.BinaryDeserializer;
import org.springframework.stereotype.Service;

@Service
public class EventPayloadParser extends MachineEventParser<EventPayload> {

    public EventPayloadParser(BinaryDeserializer<EventPayload> deserializer) {
        super(deserializer);
    }
}