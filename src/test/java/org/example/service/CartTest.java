package org.example.service;

import org.example.dto.CartItemDto;
import org.example.model.TaxMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class CartTest {

    private Cart cart;

    @BeforeEach
    void setUp() {
        cart = new Cart();
    }

    @Test
    void testAddItem() {
        CartItemDto item = new CartItemDto();
        item.setProductId(1L);
        item.setQuantity(1);

        cart.addItem(item);

        assertFalse(cart.getItems().isEmpty(), "Košík by neměl být po přidání položky prázdný.");
        assertEquals(1, cart.getItems().size(), "Očekáváme přesně 1 položku v košíku.");
    }

    @Test
    void testClear() {
        CartItemDto item = new CartItemDto();
        item.setProductId(1L);
        item.setQuantity(1);

        cart.addItem(item);
        cart.clear();

        assertTrue(cart.getItems().isEmpty(), "Košík musí být po zavolání clear() prázdný.");
    }

    @Test
    void testRequiresManufacturing_TrueWhenQuantityExceedsStock() {
        CartItemDto item = new CartItemDto();
        item.setQuantity(5);
        item.setStockQuantity(2.0);

        assertTrue(item.isRequiresManufacturing(), "Mělo by vyžadovat výrobu, protože požadované množství (5) > sklad (2.0)");
    }

    @Test
    void testRequiresManufacturing_FalseWhenQuantityWithinStock() {
        CartItemDto item = new CartItemDto();
        item.setQuantity(2);
        item.setStockQuantity(5.0);

        assertFalse(item.isRequiresManufacturing(), "Nemělo by vyžadovat výrobu, protože požadované množství (2) <= sklad (5.0)");
    }

    @Test
    void testRequiresManufacturing_UpdatesCorrectlyWhenQuantityChangedInCart() {
        CartItemDto item = new CartItemDto();
        item.setProductId(1L);
        item.setQuantity(2);
        item.setStockQuantity(3.0);
        cart.addItem(item);

        CartItemDto item2 = new CartItemDto();
        item2.setProductId(1L);
        item2.setQuantity(2);
        item2.setStockQuantity(3.0);
        cart.addItem(item2);

        CartItemDto cartItem = cart.getItems().get(0);
        assertEquals(4, cartItem.getQuantity(), "Množství by se mělo sečíst na 4");
        assertTrue(cartItem.isRequiresManufacturing(), "Po navýšení množství v košíku na 4 by položka měla vyžadovat výrobu (sklad 3.0)");
    }

    @Test
    void testCartTaxMode_StandardModeIsDefault() {
        assertEquals(TaxMode.STANDARD, cart.getTaxMode(), "Výchozí režim košíku musí být STANDARD");
    }

    @Test
    void testCartTaxMode_ReducedModeOverridesTaxRate() {
        CartItemDto item = new CartItemDto();
        item.setProductId(1L);
        item.setQuantity(1);
        item.setBasePrice(new BigDecimal("100.00"));
        item.setTaxRateValue(new BigDecimal("21.00"));

        cart.addItem(item);
        cart.setTaxMode(TaxMode.REDUCED);

        Map<BigDecimal, BigDecimal> taxBreakdown = cart.getTaxBreakdown();

        assertTrue(taxBreakdown.containsKey(new BigDecimal("12.00")), "Rozpad DPH musí obsahovat 12 %");
        assertFalse(taxBreakdown.containsKey(new BigDecimal("21.00")), "21 % DPH se nesmí aplikovat");
        assertEquals(0, new BigDecimal("12.00").compareTo(taxBreakdown.get(new BigDecimal("12.00"))), "Hodnota DPH musí být 12 Kč");
        assertEquals(0, new BigDecimal("112.00").compareTo(cart.getTotalPrice()), "Celková cena musí odpovídat 12 % DPH");
    }

    @Test
    void testCartTaxMode_StandardModeUsesOriginalTaxRate() {
        CartItemDto item = new CartItemDto();
        item.setProductId(1L);
        item.setQuantity(1);
        item.setBasePrice(new BigDecimal("100.00"));
        item.setTaxRateValue(new BigDecimal("21.00"));

        cart.addItem(item);
        cart.setTaxMode(TaxMode.STANDARD);

        Map<BigDecimal, BigDecimal> taxBreakdown = cart.getTaxBreakdown();

        assertTrue(taxBreakdown.containsKey(new BigDecimal("21.00")), "Rozpad DPH musí obsahovat 21 %");
        assertEquals(0, new BigDecimal("21.00").compareTo(taxBreakdown.get(new BigDecimal("21.00"))), "Hodnota DPH musí být 21 Kč");
        assertEquals(0, new BigDecimal("121.00").compareTo(cart.getTotalPrice()), "Celková cena musí odpovídat 21 % DPH");
    }
}