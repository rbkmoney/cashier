package com.rbkmoney.cashier.service;

import com.rbkmoney.cashier.domain.CashRegister;
import com.rbkmoney.cashier.util.JsonMapper;
import com.rbkmoney.damsel.claim_management.InvalidChangeset;
import com.rbkmoney.damsel.claim_management.Modification;
import com.rbkmoney.damsel.domain.*;
import com.rbkmoney.damsel.domain_config.RepositoryClientSrv;
import com.rbkmoney.damsel.domain_config.VersionedObject;
import org.apache.thrift.TException;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.rbkmoney.damsel.domain.ProviderParameterType.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class CashRegisterValidatorTest {

    private CashRegisterValidator validator;

    @Before
    public void setUp() throws TException {
        RepositoryClientSrv.Iface dominantClient = mock(RepositoryClientSrv.Iface.class);
        when(dominantClient.checkoutObject(any(), any()))
                .thenReturn(versionedObject());

        validator = new CashRegisterValidator(dominantClient);
    }

    @Test
    public void shouldValidateCorrectCashRegister() throws TException {
        // Given
        Modification modification = new Modification();
        Map<String, String> providerParams = Map.of(
                "name", "pupa",
                "password", "lupa",
                "tel", "88005553535",
                "url", "https://pupalupa.com/");
        CashRegister cashRegister = CashRegister.builder()
                .providerId(1)
                .providerParams(JsonMapper.toJson(providerParams))
                .build();

        // When
        validator.validate(Map.of(modification, cashRegister));

        // Then – no exception
    }

    @Test (expected = InvalidChangeset.class)
    public void shouldValidateIncorrectCashRegister() throws TException {
        // Given
        Modification modification = new Modification();
        Map<String, String> providerParams = Map.of(
                "tel", "muuu",
                "url", "muuu:||pupalupa.mu|");
        CashRegister cashRegister = CashRegister.builder()
                .providerId(1)
                .providerParams(JsonMapper.toJson(providerParams))
                .build();

        // When
        validator.validate(Map.of(modification, cashRegister));

        // Then - InvalidChangeset exception
    }

    private VersionedObject versionedObject() {
        List<ProviderParameter> paramsSchema = List.of(
                new ProviderParameter()
                        .setId("name")
                        .setIsRequired(true)
                        .setType(string_type(new ProviderParameterString())),
                new ProviderParameter()
                        .setId("password")
                        .setIsRequired(false)
                        .setType(password_type(new ProviderParameterPassword())),
                new ProviderParameter()
                        .setId("tel")
                        .setIsRequired(true)
                        .setType(integer_type(new ProviderParameterInteger())),
                new ProviderParameter()
                        .setId("url")
                        .setIsRequired(true)
                        .setType(url_type(new ProviderParameterUrl())));

        return new VersionedObject()
                .setObject(DomainObject.cash_register_provider(new CashRegisterProviderObject()
                        .setData(new CashRegisterProvider()
                                .setName("PUPA_LUPA")
                                .setParamsSchema(paramsSchema))));
    }
}
