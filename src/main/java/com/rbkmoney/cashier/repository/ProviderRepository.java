package com.rbkmoney.cashier.repository;

import com.rbkmoney.cashier.exception.ProviderNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Slf4j
@Repository
public class ProviderRepository {

    // TODO [a.romanov]: implementation
    public String findBy() {
        log.debug("Looking for provider with ={}", "");

        String providerId = "";

        return Optional.ofNullable(providerId)
                .orElseThrow(() -> new ProviderNotFoundException(""));
    }
}
