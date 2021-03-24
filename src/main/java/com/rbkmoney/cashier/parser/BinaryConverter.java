package com.rbkmoney.cashier.parser;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import lombok.extern.slf4j.Slf4j;
import org.apache.thrift.TDeserializer;
import org.apache.thrift.TException;
import org.apache.thrift.protocol.TBinaryProtocol;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class BinaryConverter {

    private ThreadLocal<TDeserializer> thriftDeserializerThreadLocal = ThreadLocal.withInitial(
            () -> new TDeserializer(
                    new TBinaryProtocol.Factory()));

    EventPayload convert(byte[] bin) {
        EventPayload event = new EventPayload();

        try {
            thriftDeserializerThreadLocal.get().deserialize(event, bin);
        } catch (TException e) {
            log.error("BinaryConverter exception: ", e);
        }

        return event;
    }
}
