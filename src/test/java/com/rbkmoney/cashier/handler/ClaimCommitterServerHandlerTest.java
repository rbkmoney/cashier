package com.rbkmoney.cashier.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.cashier.domain.CashRegister;
import com.rbkmoney.cashier.repository.CashRegisterRepository;
import com.rbkmoney.damsel.claim_management.*;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class ClaimCommitterServerHandlerTest {

    private CashRegisterRepository cashRegisterRepository;
    private ClaimCommitterServerHandler claimCommitterServerHandler;

    private ArgumentCaptor<CashRegister> cashRegisterCaptor;

    @Before
    public void setUp() {
        cashRegisterCaptor = ArgumentCaptor.forClass(CashRegister.class);
        cashRegisterRepository = mock(CashRegisterRepository.class);
        claimCommitterServerHandler = new ClaimCommitterServerHandler(
                cashRegisterRepository,
                new ObjectMapper());
    }

    @Test
    public void shouldCommitCashRegisterClaim() {
        // Given
        Claim claim = new Claim()
                .setChangeset(List.of(new ModificationUnit()
                        .setModification(Modification.party_modification(
                                PartyModification.shop_modification(new ShopModificationUnit()
                                        .setId("shop-id")
                                        .setModification(
                                                ShopModification.cash_register_modification_unit(new CashRegisterModificationUnit()
                                                        .setId("cash-register-id")
                                                        .setModification(CashRegisterModification.creation(new CashRegisterParams()
                                                                .setCashRegisterProviderId(1)
                                                                .setCashRegisterProviderParams(Map.of("a", "b")))))))))));

        // When
        claimCommitterServerHandler.commit("party-id", claim);

        // Then
        verify(cashRegisterRepository, only())
                .save(cashRegisterCaptor.capture());
        CashRegister cashRegister = cashRegisterCaptor.getValue();

        CashRegister expected = CashRegister.builder()
                .id("cash-register-id")
                .partyId("party-id")
                .shopId("shop-id")
                .providerId(1)
                .providerParams("{\"a\":\"b\"}")
                .build();

        assertThat(cashRegister).isEqualTo(expected);
    }
}