package com.rbkmoney.cashier.config.properties;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "kafka.ssl")
public class KafkaSslProperties {

    private boolean enabled;

    private String keyStoreType;
    private String keyStoreLocation;
    private String keyStorePassword;
    private String keyPassword;

    private String trustStoreType;
    private String trustStoreLocation;
    private String trustStorePassword;
}
