package com.rbkmoney.cashier.serde;

import com.rbkmoney.kafka.common.serialization.AbstractThriftDeserializer;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MachineEventDeserializer extends AbstractThriftDeserializer<MachineEvent> {

    @Override
    public MachineEvent deserialize(String topic, byte[] data) {
        return deserialize(data, new MachineEvent());
    }
}
