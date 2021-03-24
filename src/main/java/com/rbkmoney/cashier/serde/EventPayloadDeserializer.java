package com.rbkmoney.cashier.serde;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.sink.common.serialization.impl.AbstractThriftBinaryDeserializer;
import org.springframework.stereotype.Service;

@Service
public class EventPayloadDeserializer extends AbstractThriftBinaryDeserializer<EventPayload> {

    @Override
    public EventPayload deserialize(byte[] bin) {
        return deserialize(bin, new EventPayload());
    }
}
