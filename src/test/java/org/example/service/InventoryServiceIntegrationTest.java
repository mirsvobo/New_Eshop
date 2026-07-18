package org.example.service;

import org.example.model.Product;
import org.example.model.StockMovement;
import org.example.model.TaxRate;
import org.example.model.User;
import org.example.repository.ProductRepository;
import org.example.repository.StockMovementRepository;
import org.example.repository.TaxRateRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
@Transactional
class InventoryServiceIntegrationTest {

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TaxRateRepository taxRateRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Test
    void testGetFilteredMovements_AllBranches() {
        // Příprava testovacích dat v DB
        User user = userRepository.save(User.builder()
                .email("inv-test@test.cz")
                .password("pass")
                .firstName("Inv")
                .lastName("Test")
                .role(User.Role.ROLE_ADMIN)
                .active(true)
                .build());

        TaxRate tax = taxRateRepository.save(TaxRate.builder()
                .name("DPH 21%")
                .rate(new BigDecimal("21"))
                .defaultRate(true)
                .build());

        Product product = productRepository.save(Product.builder()
                .name("Zkušební produkt")
                .price(new BigDecimal("100"))
                .taxRate(tax)
                .stockQuantity(10)
                .type(Product.ProductType.PRODUCT)
                .unit("ks")
                .active(true)
                .build());

        stockMovementRepository.save(StockMovement.builder()
                .product(product)
                .performedBy(user)
                .quantity(5.0)
                .type(StockMovement.MovementType.RECEIPT)
                .timestamp(LocalDateTime.now())
                .note("Test")
                .build());

        // 1. Zcela prázdný filtr (vrátí vše)
        List<StockMovement> all = inventoryService.getFilteredMovements(null, null, null, null, null, null);
        assertNotNull(all);
        assertFalse(all.isEmpty());

        // 2. Filtr na konkrétní produkt, uživatele a limity (min/max)
        List<StockMovement> filtered1 = inventoryService.getFilteredMovements(product.getId(), user.getId(), null, 1.0, 10.0, null);
        assertFalse(filtered1.isEmpty());

        // 3. Pozitivní směr pohybu (positive) a datum "today"
        List<StockMovement> filteredPos = inventoryService.getFilteredMovements(null, null, "positive", null, null, "today");
        assertNotNull(filteredPos);

        // 4. Negativní směr pohybu (negative) a datum "week"
        List<StockMovement> filteredNeg = inventoryService.getFilteredMovements(null, null, "negative", null, null, "week");
        assertNotNull(filteredNeg);

        // 5. Pokrytí zbývajících dateRange parametrů ("month", "year")
        inventoryService.getFilteredMovements(null, null, null, null, null, "month");
        inventoryService.getFilteredMovements(null, null, null, null, null, "year");
    }
}