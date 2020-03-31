package com.rbkmoney.cashier;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;


@ServletComponentScan
@SpringBootApplication
public class CashierApplication extends SpringApplication {

    public static void main(String[] args) {
        SpringApplication.run(CashierApplication.class, args);
    }
}
