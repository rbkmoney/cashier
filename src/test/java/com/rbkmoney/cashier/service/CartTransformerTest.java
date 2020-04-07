package com.rbkmoney.cashier.service;

import com.rbkmoney.damsel.cashreg.receipt.ItemsLine;
import com.rbkmoney.damsel.domain.Cash;
import com.rbkmoney.damsel.domain.CurrencyRef;
import com.rbkmoney.damsel.domain.InvoiceCart;
import com.rbkmoney.damsel.domain.InvoiceLine;
import com.rbkmoney.damsel.msgpack.Value;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class CartTransformerTest {

    private static final String TEST_PRODUCT = "TEST_PRODUCT";
    private static final String TEST_TAX = "TEST_TAX";


    private CartTransformer cartTransformer;

    @Before
    public void setUp() {
        cartTransformer = new CartTransformer();
    }

    @Test
    public void shouldHandleNullCart() {
        // Given - When
        List<ItemsLine> items = cartTransformer.transform(null);

        // Then
        assertThat(items).isEmpty();
    }

    @Test
    public void shouldHandleEmptyCart() {
        // Given
        InvoiceCart invoiceCart = new InvoiceCart()
                .setLines(emptyList());

        // When
        List<ItemsLine> items = cartTransformer.transform(invoiceCart);

        // Then
        assertThat(items).isEmpty();
    }

    @Test
    public void shouldTransformInvoiceCartToItemsLines() {
        // Given
        InvoiceCart invoiceCart = new InvoiceCart()
                .setLines(List.of(
                        new InvoiceLine()
                                .setProduct("\"" + TEST_PRODUCT + "\"")
                                .setPrice(new Cash()
                                        .setAmount(100L)
                                        .setCurrency(new CurrencyRef()
                                                .setSymbolicCode("RUB")))
                                .setQuantity(1)
                                .setMetadata(Map.of("TaxMode", Value.str(TEST_TAX)))));

        // When
        List<ItemsLine> items = cartTransformer.transform(invoiceCart);

        // Then
        assertThat(items).hasSize(1);

        ItemsLine item = items.get(0);
        assertThat(item.getProduct()).isEqualTo(TEST_PRODUCT);
        assertThat(item.getPrice().getAmount()).isEqualTo(100L);
        assertThat(item.getPrice().getCurrency().getSymbolicCode()).isEqualTo("RUB");
        assertThat(item.getQuantity()).isEqualTo(1);
        assertThat(item.getTax()).isEqualTo(TEST_TAX);
    }
}