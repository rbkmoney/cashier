package com.rbkmoney.cashier.parser;

import com.rbkmoney.damsel.payment_processing.EventPayload;
import com.rbkmoney.machinegun.eventsink.MachineEvent;
import com.rbkmoney.sink.common.exception.ParseException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SourceEventParser {

    private final BinaryConverter converter;

    public EventPayload parse(MachineEvent message) {
        try {
            byte[] bin = message.getData().getBin();
            return converter.convert(bin);
        } catch (Exception e) {
            throw new ParseException(e);
        }
    }
}
