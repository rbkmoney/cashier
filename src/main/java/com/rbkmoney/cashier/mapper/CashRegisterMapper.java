package com.rbkmoney.cashier.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.cashier.domain.CashRegister;
import com.rbkmoney.damsel.cashreg.processing.CashRegisterProvider;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class CashRegisterMapper {

    private final ObjectMapper objectMapper;

    public List<CashRegisterProvider> map(List<CashRegister> cashRegisters) {
        return cashRegisters.stream()
                .map(this::map)
                .collect(toList());
    }

    @SneakyThrows
    @SuppressWarnings("unchecked")
    public CashRegisterProvider map(CashRegister cashRegister) {
        Map<String, String> params = objectMapper.readValue(cashRegister.getProviderParams(), Map.class);

        return new CashRegisterProvider()
                .setProviderId(String.valueOf(cashRegister.getProviderId()))
                .setProviderParams(params);
    }
}
