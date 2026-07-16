package org.example.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class ProductTest {
    @Test
    void getPriceWithTax_CalculatesCorrectly() {
        TaxRate tax = TaxRate.builder().rate(new BigDecimal("21.0")).build();
        Product product = Product.builder()
                .price(new BigDecimal("100.00"))
                .taxRate(tax)
                .build();

        assertEquals(0, new BigDecimal("121.00").compareTo(product.getPriceWithTax()));
    }
}