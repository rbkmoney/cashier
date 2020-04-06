package com.rbkmoney.cashier.config;

import com.rbkmoney.damsel.cashreg.processing.ManagementSrv;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class CashRegConfig {

    @Bean
    public ManagementSrv.Iface cashregClient(
            @Value("${client.cashreg.url}") Resource url,
            @Value("${client.cashreg.timeout}") int timeout) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(url.getURI())
                .withNetworkTimeout(timeout)
                .build(ManagementSrv.Iface.class);
    }
}
