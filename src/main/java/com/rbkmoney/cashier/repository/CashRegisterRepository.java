package com.rbkmoney.cashier.repository;

import com.rbkmoney.cashier.domain.CashRegister;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CashRegisterRepository extends JpaRepository<CashRegister, String> {

    List<CashRegister> findByPartyIdAndShopId(String partyId, String shopId);
}
