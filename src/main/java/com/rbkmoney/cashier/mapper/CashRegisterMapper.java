package com.rbkmoney.cashier.mapper;

import com.rbkmoney.cashier.domain.CashRegister;
import com.rbkmoney.cashier.util.JsonMapper;
import com.rbkmoney.damsel.cashreg.processing.CashRegisterProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@Service
@RequiredArgsConstructor
public class CashRegisterMapper {

    public List<CashRegisterProvider> map(List<CashRegister> cashRegisters) {
        return cashRegisters.stream()
                .map(this::map)
                .collect(toList());
    }

    public CashRegisterProvider map(CashRegister cashRegister) {
        Map<String, String> params = JsonMapper.toMap(cashRegister.getProviderParams());

        return new CashRegisterProvider()
                .setProviderId(String.valueOf(cashRegister.getProviderId()))
                .setProviderParams(params);
    }
}
