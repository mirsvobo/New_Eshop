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
    void testAddItem_WithProductConfigurationsAndImage() {
        CartItemDto item = new CartItemDto();
        item.setProductId(1L);
        item.setQuantity(1);
        item.setSelectedLazure("Dub");
        item.setSelectedRoofColor("Červená");
        item.setSelectedDesign("Modern");
        item.setImageUrl("/images/test-image.jpg");

        cart.addItem(item);

        assertFalse(cart.getItems().isEmpty(), "Košík by neměl být po přidání položky prázdný.");
        CartItemDto cartItem = cart.getItems().get(0);
        assertEquals("Dub", cartItem.getSelectedLazure(), "Vybraná lazura se musí uložit do košíku.");
        assertEquals("Červená", cartItem.getSelectedRoofColor(), "Vybraná barva střechy se musí uložit do košíku.");
        assertEquals("Modern", cartItem.getSelectedDesign(), "Vybraný design se musí uložit do košíku.");
        assertEquals("/images/test-image.jpg", cartItem.getImageUrl(), "Obrázek se musí správně uložit do košíku.");
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
    @Test
    void testUpdateQuantity_UpdatesExistingItem() {
        CartItemDto item = new CartItemDto();
        item.setProductId(1L);
        item.setQuantity(2);
        cart.addItem(item);

        cart.updateQuantity(1L, 5);

        assertEquals(5, cart.getItems().get(0).getQuantity(), "Množství by mělo být aktualizováno na 5.");
    }

    @Test
    void testUpdateQuantity_ZeroOrNegative_RemovesItem() {
        CartItemDto item = new CartItemDto();
        item.setProductId(1L);
        item.setQuantity(2);
        cart.addItem(item);

        cart.updateQuantity(1L, 0);

        assertTrue(cart.getItems().isEmpty(), "Položka s nulovým nebo záporným množstvím by měla být z košíku odstraněna.");
    }

    @Test
    void testGetTotalItems_ReturnsCorrectSum() {
        CartItemDto item1 = new CartItemDto();
        item1.setProductId(1L);
        item1.setQuantity(2);

        CartItemDto item2 = new CartItemDto();
        item2.setProductId(2L);
        item2.setQuantity(3);

        cart.addItem(item1);
        cart.addItem(item2);

        assertEquals(5, cart.getTotalItems(), "Celkový počet položek v košíku musí být 5.");
    }

    @Test
    void testApplyAndRemoveCoupon() {
        org.example.model.Coupon coupon = new org.example.model.Coupon();
        coupon.setCode("TEST10");

        cart.applyCoupon(coupon);
        assertEquals(coupon, cart.getAppliedCoupon(), "Kupón musí být úspěšně aplikován do košíku.");

        cart.removeCoupon();
        assertNull(cart.getAppliedCoupon(), "Kupón musí být úspěšně odstraněn z košíku.");
    }

    @Test
    void testGetDiscountAmount_And_FinalPrice_PercentageCoupon() {
        CartItemDto item = new CartItemDto();
        item.setProductId(1L);
        item.setQuantity(1);
        item.setBasePrice(new BigDecimal("100.00"));
        item.setTaxRateValue(new BigDecimal("21.00")); // Celková cena s DPH je 121.00
        cart.addItem(item);

        org.example.model.Coupon coupon = new org.example.model.Coupon();
        coupon.setType(org.example.model.DiscountType.PERCENTAGE);
        coupon.setDiscountValue(new BigDecimal("10.00")); // Sleva 10%
        cart.applyCoupon(coupon);

        assertEquals(0, new BigDecimal("12.10").compareTo(cart.getDiscountAmount()), "Sleva musí činit 10% z 121.00.");
        assertEquals(0, new BigDecimal("108.90").compareTo(cart.getFinalPrice()), "Konečná cena musí být po odečtení slevy 108.90.");
    }

    @Test
    void testGetDiscountAmount_ReducedTaxMode_FixedCouponExceedingTotal() {
        CartItemDto item = new CartItemDto();
        item.setProductId(1L);
        item.setQuantity(1);
        item.setBasePrice(new BigDecimal("100.00"));
        item.setTaxRateValue(new BigDecimal("21.00"));
        cart.addItem(item);

        // Zapneme sníženou sazbu - dynamická cena bude 112.00
        cart.setTaxMode(TaxMode.REDUCED);

        org.example.model.Coupon coupon = new org.example.model.Coupon();
        coupon.setType(org.example.model.DiscountType.FIXED);
        coupon.setDiscountValue(new BigDecimal("200.00")); // Sleva větší než celková cena
        cart.applyCoupon(coupon);

        assertEquals(0, new BigDecimal("112.00").compareTo(cart.getDiscountAmount()), "Sleva nesmí přesáhnout celkovou hodnotu nákupu.");
        assertEquals(0, BigDecimal.ZERO.compareTo(cart.getFinalPrice()), "Konečná cena nesmí jít do mínusu, minimum je 0.");
    }
}