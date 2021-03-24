package com.rbkmoney.cashier.serde;

import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.machinegun.eventsink.SinkEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.Deserializer;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.protocol.TBinaryProtocol;

import java.util.Map;

@Slf4j
public class MachineEventDeserializer implements Deserializer<MachineEvent> {

    private ThreadLocal<TDeserializer> thriftDeserializerThreadLocal = ThreadLocal.withInitial(
            () -> new TDeserializer(
                    new TBinaryProtocol.Factory()));

    @Override
    public MachineEvent deserialize(String topic, byte[] data) {
        SinkEvent machineEvent = new SinkEvent();

        try {
            thriftDeserializerThreadLocal.get().deserialize(machineEvent, data);
        } catch (Exception e) {
            log.error("Deserialization error. Machine event data: {} ", data, e);
        }

        return machineEvent.getEvent();
    }

    @Override
    public void configure(Map<String, ?> configs, boolean isKey) {
    }

    @Override
    public void close() {
    }
}
