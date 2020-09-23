package com.rbkmoney.cashier.domain;

import lombok.*;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.io.Serializable;

@Entity
@Builder
@Data
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "cash_register")
public class CashRegister implements Serializable {

    @Id
    private String id;

    @Column(name = "party_id")
    private String partyId;

    @Column(name = "shop_id")
    private String shopId;

    @Column(name = "provider_id")
    private int providerId;

    @ToString.Exclude
    @Column(name = "provider_params")
    private String providerParams;
}
