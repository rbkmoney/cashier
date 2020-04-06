package com.rbkmoney.cashier.mapper;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.cashier.domain.CashRegister;
import com.rbkmoney.damsel.cashreg.processing.CashRegisterProvider;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class CashRegisterMapperTest {

    private ObjectMapper objectMapper;
    private CashRegisterMapper cashRegisterMapper;

    @Before
    public void setUp() {
        objectMapper = new ObjectMapper();
        cashRegisterMapper = new CashRegisterMapper(objectMapper);
    }

    @Test
    public void shouldMapCashRegisterToCashRegisterProvider() throws JsonProcessingException {
        // Given
        Map<String, String> params = Map.of("a", "1", "b", "2", "c", "3");
        String json = objectMapper.writeValueAsString(params);

        CashRegister cashRegister = CashRegister.builder()
                .providerId(1)
                .providerParams(json)
                .build();

        // When
        CashRegisterProvider provider = cashRegisterMapper.map(cashRegister);

        // Then
        assertThat(provider.getProviderId()).isEqualTo("1");
        assertThat(provider.getProviderParams()).isEqualTo(params);
    }
}