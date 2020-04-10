package com.rbkmoney.cashier.service;

import com.rbkmoney.cashier.domain.CashRegister;
import com.rbkmoney.cashier.util.JsonMapper;
import com.rbkmoney.damsel.claim_management.InvalidChangeset;
import com.rbkmoney.damsel.claim_management.Modification;
import com.rbkmoney.damsel.domain.CashRegisterProvider;
import com.rbkmoney.damsel.domain.CashRegisterProviderParameter;
import com.rbkmoney.damsel.domain.CashRegisterProviderParameterType;
import com.rbkmoney.damsel.domain.CashRegisterProviderRef;
import com.rbkmoney.damsel.domain_config.Head;
import com.rbkmoney.damsel.domain_config.Reference;
import com.rbkmoney.damsel.domain_config.RepositoryClientSrv;
import com.rbkmoney.damsel.domain_config.VersionedObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.UrlValidator;
import org.apache.thrift.TException;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

@Slf4j
@Service
@RequiredArgsConstructor
public class CashRegisterValidator {

    private final RepositoryClientSrv.Iface dominantClient;

    public void validate(Map<Modification, CashRegister> cashRegisters) throws TException {

        for (Map.Entry<Modification, CashRegister> entry : cashRegisters.entrySet()) {
            Modification modification = entry.getKey();
            CashRegister cashRegister = entry.getValue();
            int providerId = cashRegister.getProviderId();

            VersionedObject versionedObject = dominantClient.checkoutObject(
                    Reference.head(new Head()),
                    com.rbkmoney.damsel.domain.Reference.cash_register_provider(
                            new CashRegisterProviderRef(providerId)));

            CashRegisterProvider cashRegisterProvider = versionedObject
                    .getObject()
                    .getCashRegisterProvider()
                    .getData();

            Map<String, String> errors = validate(cashRegister, cashRegisterProvider);

            if (!errors.isEmpty()) {
                log.warn("Validation failed, reason={}", errors);
                throw new InvalidChangeset(JsonMapper.toJson(errors), singletonList(modification));
            }
        }
    }

    private Map<String, String> validate(CashRegister cashRegister, CashRegisterProvider cashRegisterProvider) {
        log.info("Validating cashRegister={} using cashRegisterProvider={} schema",
                cashRegister,
                cashRegisterProvider.getName());

        Map<String, String> providerParams = JsonMapper.toMap(cashRegister.getProviderParams());
        List<CashRegisterProviderParameter> paramsSchema = cashRegisterProvider.getParamsSchema();

        Map<String, String> errors = new HashMap<>();
        for (CashRegisterProviderParameter providerParam : paramsSchema) {
            String cashRegisterParam = providerParams.get(providerParam.getId());

            if (!providerParam.isIsRequired() && cashRegisterParam == null) {
                continue;
            }

            if (providerParam.isIsRequired() && cashRegisterParam == null) {
                errors.put(providerParam.getId(), "Field must be set");
                continue;
            }

            CashRegisterProviderParameterType providerParamType = providerParam.getType();

            if (providerParamType.isSetIntegerType() && !cashRegisterParam.matches("\\d+")) {
                errors.put(providerParam.getId(), "Field must be an integer");
                continue;
            }

            if (providerParamType.isSetUrlType() && !UrlValidator.getInstance().isValid(cashRegisterParam)) {
                errors.put(providerParam.getId(), "Field must be a valid URL");
            }
        }

        return errors;
    }
}
