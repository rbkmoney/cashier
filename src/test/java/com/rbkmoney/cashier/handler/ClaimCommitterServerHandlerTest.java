package com.rbkmoney.cashier.handler;

import com.rbkmoney.cashier.domain.CashRegister;
import com.rbkmoney.cashier.repository.CashRegisterRepository;
import com.rbkmoney.cashier.service.CashRegisterValidator;
import com.rbkmoney.damsel.claim_management.*;
import com.rbkmoney.damsel.domain.CategoryRef;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class ClaimCommitterServerHandlerTest {

    @Mock private CashRegisterValidator cashRegisterValidator;
    @Mock private CashRegisterRepository cashRegisterRepository;
    @Captor private ArgumentCaptor<CashRegister> cashRegisterCaptor;

    @InjectMocks
    private ClaimCommitterServerHandler claimCommitterServerHandler;

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

    @Test
    public void shouldHandleClaimWithNoPartyModification() {
        // Given
        Claim claim = new Claim()
                .setChangeset(List.of(new ModificationUnit()
                        .setModification(Modification.claim_modification(new ClaimModification()))));

        // When
        claimCommitterServerHandler.commit("party-id", claim);

        // Then
        verify(cashRegisterRepository, never())
                .save(any());
    }

    @Test
    public void shouldHandleClaimWithNoShopModification() {
        // Given
        Claim claim = new Claim()
                .setChangeset(List.of(new ModificationUnit()
                        .setModification(Modification.party_modification(
                                PartyModification.contract_modification(new ContractModificationUnit())))));

        // When
        claimCommitterServerHandler.commit("party-id", claim);

        // Then
        verify(cashRegisterRepository, never())
                .save(any());
    }

    @Test
    public void shouldHandleClaimWithNoCashRegisterModification() {
        // Given
        Claim claim = new Claim()
                .setChangeset(List.of(new ModificationUnit()
                        .setModification(Modification.party_modification(
                                PartyModification.shop_modification(new ShopModificationUnit()
                                        .setId("shop-id")
                                        .setModification(
                                                ShopModification.category_modification(new CategoryRef())))))));

        // When
        claimCommitterServerHandler.commit("party-id", claim);

        // Then
        verify(cashRegisterRepository, never())
                .save(any());
    }
}