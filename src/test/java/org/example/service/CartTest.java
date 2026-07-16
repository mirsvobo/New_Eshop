package org.example.service;

import org.example.dto.CartItemDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

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

    // --- NOVÉ TESTY PRO VÝROBU (RED PHASE) ---

    @Test
    void testRequiresManufacturing_TrueWhenQuantityExceedsStock() {
        CartItemDto item = new CartItemDto();
        item.setQuantity(5);
        item.setStockQuantity(2.0); // Přidáváme znalost o stavu skladu do DTO

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

        // Zákazník přidá další 2 kusy stejného produktu (celkem 4, ale na skladě jsou jen 3)
        CartItemDto item2 = new CartItemDto();
        item2.setProductId(1L);
        item2.setQuantity(2);
        item2.setStockQuantity(3.0);

        cart.addItem(item2);

        CartItemDto cartItem = cart.getItems().get(0);
        assertEquals(4, cartItem.getQuantity(), "Množství by se mělo sečíst na 4");
        assertTrue(cartItem.isRequiresManufacturing(), "Po navýšení množství v košíku na 4 by položka měla vyžadovat výrobu (sklad 3.0)");
    }
}