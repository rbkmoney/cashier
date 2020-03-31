package com.rbkmoney.cashier.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.cashier.domain.CashRegister;
import com.rbkmoney.cashier.repository.CashRegisterRepository;
import com.rbkmoney.damsel.claim_management.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ClaimCommitterServerHandler implements ClaimCommitterSrv.Iface {

    private final CashRegisterRepository cashRegisterRepository;
    private final ObjectMapper objectMapper;

    @Override
    public void accept(String partyId, Claim claim) throws PartyNotFound, InvalidChangeset, TException {
        // TODO [a.romanov]: validate using dominant
    }

    @Override
    public void commit(String partyId, Claim claim) {
        claim.getChangeset()
                .forEach(modificationUnit -> findCashRegister(partyId, modificationUnit)
                        .ifPresent(cashRegisterRepository::save));
    }

    private Optional<CashRegister> findCashRegister(String partyId, ModificationUnit modificationUnit) {
        CashRegister.CashRegisterBuilder builder = CashRegister.builder();
        builder.partyId(partyId);

        Modification modification = modificationUnit.getModification();
        return findInPartyModification(builder, modification);
    }

    private Optional<CashRegister> findInPartyModification(
            CashRegister.CashRegisterBuilder builder,
            Modification modification) {
        if (!modification.isSetPartyModification()) {
            return Optional.empty();
        }

        return findInShopModification(modification.getPartyModification(), builder);
    }

    private Optional<CashRegister> findInShopModification(
            PartyModification partyModification,
            CashRegister.CashRegisterBuilder builder) {
        if (!partyModification.isSetShopModification()) {
            return Optional.empty();
        }

        ShopModificationUnit shopModificationUnit = partyModification.getShopModification();
        builder.shopId(shopModificationUnit.getId());

        return findInCashRegisterModification(shopModificationUnit.getModification(), builder);
    }

    private Optional<CashRegister> findInCashRegisterModification(
            ShopModification shopModification,
            CashRegister.CashRegisterBuilder builder) {
        if (!shopModification.isSetCashRegisterModificationUnit()) {
            return Optional.empty();
        }

        CashRegisterModificationUnit cashRegisterModificationUnit = shopModification.getCashRegisterModificationUnit();
        builder.id(cashRegisterModificationUnit.getId());

        return findInCreationModification(cashRegisterModificationUnit.getModification(), builder);
    }

    @SneakyThrows
    private Optional<CashRegister> findInCreationModification(
            CashRegisterModification cashRegisterModification,
            CashRegister.CashRegisterBuilder builder) {
        if (!cashRegisterModification.isSetCreation()) {
            return Optional.empty();
        }

        CashRegisterParams cashRegisterParams = cashRegisterModification.getCreation();
        builder.providerId(cashRegisterParams.getCashRegisterProviderId());
        builder.providerParams(objectMapper.writeValueAsString(cashRegisterParams.getCashRegisterProviderParams()));

        return Optional.of(builder.build());
    }
}
