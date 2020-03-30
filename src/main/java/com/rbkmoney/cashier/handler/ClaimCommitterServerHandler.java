package com.rbkmoney.cashier.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rbkmoney.cashier.domain.CashRegister;
import com.rbkmoney.cashier.repository.CashRegisterRepository;
import com.rbkmoney.damsel.claim_management.*;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.Map;

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
    @SneakyThrows
    public void commit(String partyId, Claim claim) {
        for (ModificationUnit modificationUnit : claim.getChangeset()) {
            Modification modification = modificationUnit.getModification();

            if (modification.isSetPartyModification()) {
                PartyModification partyModification = modification.getPartyModification();

                if (partyModification.isSetShopModification()) {
                    ShopModificationUnit shopModificationUnit = partyModification.getShopModification();
                    String shopId = shopModificationUnit.getId();
                    ShopModification shopModification = shopModificationUnit.getModification();

                    if (shopModification.isSetCashRegisterModificationUnit()) {
                        CashRegisterModificationUnit cashRegisterModificationUnit = shopModification.getCashRegisterModificationUnit();
                        String cashRegisterId = cashRegisterModificationUnit.getId();
                        CashRegisterModification cashRegisterModification = cashRegisterModificationUnit.getModification();

                        if (cashRegisterModification.isSetCreation()) {
                            CashRegisterParams cashRegisterParams = cashRegisterModification.getCreation();
                            long providerId = cashRegisterParams.getCashRegisterProviderId();
                            Map<String, String> providerParams = cashRegisterParams.getCashRegisterProviderParams();
                            CashRegister cashRegister = CashRegister.builder()
                                    .id(cashRegisterId)
                                    .partyId(partyId)
                                    .shopId(shopId)
                                    .providerId(providerId)
                                    .providerParams(objectMapper.writeValueAsString(providerParams))
                                    .build();

                            cashRegisterRepository.save(cashRegister);
                        }
                    }
                }
            }
        }
    }
}
