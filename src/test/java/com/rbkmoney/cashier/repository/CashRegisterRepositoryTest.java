package com.rbkmoney.cashier.repository;

import com.rbkmoney.cashier.CashierApplication;
import com.rbkmoney.cashier.domain.CashRegister;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.testcontainers.containers.PostgreSQLContainer;

import java.time.Duration;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = RANDOM_PORT)
@ContextConfiguration(classes = CashierApplication.class, initializers = CashRegisterRepositoryTest.Initializer.class)
public class CashRegisterRepositoryTest {

    @ClassRule
    @SuppressWarnings("rawtypes")
    public static PostgreSQLContainer postgres = new PostgreSQLContainer<>("postgres:9.6")
            .withStartupTimeout(Duration.ofMinutes(5));

    @LocalServerPort
    protected int port;

    @Autowired
    private CashRegisterRepository cashRegisterRepository;

    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        @Override
        public void initialize(ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertyValues.of(
                    "spring.datasource.url=" + postgres.getJdbcUrl(),
                    "spring.datasource.username=" + postgres.getUsername(),
                    "spring.datasource.password=" + postgres.getPassword(),
                    "spring.flyway.url=" + postgres.getJdbcUrl(),
                    "spring.flyway.user=" + postgres.getUsername(),
                    "spring.flyway.password=" + postgres.getPassword(),
                    "kafka.auto-startup=false")
                    .and(configurableApplicationContext.getEnvironment().getActiveProfiles())
                    .applyTo(configurableApplicationContext);
        }
    }

    @Test
    public void shouldSaveCashRegister() {
        // Given
        CashRegister cashRegister = CashRegister.builder()
                .id("0")
                .partyId("party-id-0")
                .shopId("shop-id-0")
                .providerId(1)
                .providerParams("{\"a\": \"b\"}")
                .build();

        // When
        cashRegisterRepository.save(cashRegister);
        Optional<CashRegister> result = cashRegisterRepository.findById("0");

        // Then
        assertTrue(result.isPresent());
        assertThat(result.get()).isEqualTo(cashRegister);
    }

    @Test
    public void shouldFindCashRegistersByPartyIdAndShopId() {
        // Given
        CashRegister firstCashRegister = CashRegister.builder()
                .id("1")
                .partyId("party-id-2")
                .shopId("shop-id-2")
                .providerId(1)
                .providerParams("{\"a\": \"b\"}")
                .build();

        CashRegister secondCashRegister = CashRegister.builder()
                .id("2")
                .partyId("party-id-2")
                .shopId("shop-id-2")
                .providerId(2)
                .providerParams("{\"c\": \"d\"}")
                .build();

        CashRegister wrongCashRegister = CashRegister.builder()
                .id("3")
                .partyId("some-other-party-id")
                .shopId("some-other-shop-id")
                .providerId(2)
                .providerParams("{\"c\": \"d\"}")
                .build();

        // When
        cashRegisterRepository.saveAll(List.of(firstCashRegister, secondCashRegister, wrongCashRegister));
        List<CashRegister> result = cashRegisterRepository.findByPartyIdAndShopId("party-id-2", "shop-id-2");

        // Then
        assertThat(result).hasSize(2);
        assertThat(result.get(0)).isEqualTo(firstCashRegister);
        assertThat(result.get(1)).isEqualTo(secondCashRegister);
    }
}