package com.rbkmoney.cashier.service;

import com.rbkmoney.damsel.cashreg.ItemsLine;
import com.rbkmoney.damsel.domain.InvoiceCart;
import com.rbkmoney.damsel.domain.InvoiceLine;
import com.rbkmoney.damsel.msgpack.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;

@Service
class CartTransformer {

    private static final String TAX_MODE = "TaxMode";

    List<ItemsLine> transform(InvoiceCart cart) {
        return Optional.ofNullable(cart)
                .map(InvoiceCart::getLines)
                .orElse(emptyList())
                .stream()
                .map(this::toItemsLine)
                .collect(toList());
    }

    private ItemsLine toItemsLine(InvoiceLine item) {
        String tax = Optional.ofNullable(item.getMetadata())
                .map(metadata -> metadata.get(TAX_MODE))
                .map(Value::getStr)
                .orElse(null);

        return new ItemsLine()
                .setProduct(removeSpecialSymbols(item.getProduct()))
                .setPrice(item.getPrice())
                .setQuantity(item.getQuantity())
                .setTax(tax);
    }

    private String removeSpecialSymbols(String product) {
        return product.replaceAll("[\\\\\"\':/]", "");
    }
}
