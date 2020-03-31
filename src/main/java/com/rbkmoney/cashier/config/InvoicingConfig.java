package com.rbkmoney.cashier.config;

import com.rbkmoney.damsel.payment_processing.InternalUser;
import com.rbkmoney.damsel.payment_processing.InvoicingSrv;
import com.rbkmoney.damsel.payment_processing.UserInfo;
import com.rbkmoney.damsel.payment_processing.UserType;
import com.rbkmoney.woody.thrift.impl.http.THSpawnClientBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;

@Configuration
public class InvoicingConfig {

    @Bean
    public InvoicingSrv.Iface invoicingClient(
            @Value("${client.invoicing.url}") Resource url,
            @Value("${client.invoicing.timeout}") int timeout) throws IOException {
        return new THSpawnClientBuilder()
                .withAddress(url.getURI())
                .withNetworkTimeout(timeout)
                .build(InvoicingSrv.Iface.class);
    }

    @Bean
    public UserInfo invoicingAdmin() {
        return new UserInfo(
                "admin",
                UserType.internal_user(new InternalUser()));
    }
}
