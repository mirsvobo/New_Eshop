package org.example.config;

import org.example.model.Order;
import org.example.model.Product;
import org.example.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class DataSeederTest {

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private ProductRepository productRepository;
    @Autowired
    private StockMovementRepository stockMovementRepository;
    @Autowired
    private AuditLogRepository auditLogRepository;
    @Autowired
    private OrderStatusRepository orderStatusRepository;
    @Autowired
    private TaxRateRepository taxRateRepository;
    @Autowired
    private RecipeItemRepository recipeItemRepository;
    @Autowired
    private OrderRepository orderRepository;
    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Test
    void seedUsers_DatabaseIsPopulated() {
        long userCount = userRepository.count();
        assertEquals(11, userCount, "Celkový počet uživatelů neodpovídá novému seederu (5 zaměstnanců + 6 zákazníků)");
    }

    @Test
    void seedProducts_DatabaseIsPopulated() {
        long productCount = productRepository.count();
        assertEquals(10, productCount, "Celkový počet položek v katalogu by měl být 10 (6x materiál + 4x dřevníky)");
    }

    @Test
    void seedProducts_DatabaseIsPopulatedWithVariants() {
        List<Product> products = productRepository.findAll();
        boolean hasVariants = products.stream()
                .anyMatch(p -> p.getAvailableLazures() != null && !p.getAvailableLazures().isEmpty());
        assertTrue(hasVariants, "Seeder by měl vytvořit alespoň jeden produkt s dostupnými variantami.");
    }

    @Test
    void seedProducts_DatabaseIsPopulatedWithDimensions() {
        List<Product> products = productRepository.findAll();
        boolean hasDimensions = products.stream()
                .anyMatch(p -> p.getWidth() != null && p.getDepth() != null && p.getHeight() != null && p.getVolume() != null);
        assertTrue(hasDimensions, "Seeder by měl vytvořit alespoň jeden produkt s vyplněnými fyzickými rozměry.");
    }

    @Test
    void seedStockMovements_DatabaseIsPopulated() {
        long movementCount = stockMovementRepository.count();
        assertTrue(movementCount >= 10, "Měl by existovat pohyb skladu pro každý vygenerovaný produkt a materiál");

        boolean auditExists = auditLogRepository.findAll().stream()
                .anyMatch(log -> "INITIAL_SEED".equals(log.getAction()));
        assertTrue(auditExists, "Chybí auditní stopa po úvodním naskladnění (INITIAL_SEED)");
    }

    @Test
    void seedOrderStatuses_DatabaseIsPopulated() {
        long statusCount = orderStatusRepository.count();
        assertEquals(8, statusCount, "Počet stavů objednávek v databázi nesouhlasí");
    }

    @Test
    void seedTaxRates_DatabaseIsPopulated() {
        long taxCount = taxRateRepository.count();
        assertEquals(3, taxCount, "Počet daňových sazeb v databázi nesouhlasí");
    }

    @Test
    void seedRecipes_DatabaseIsPopulated() {
        long recipeCount = recipeItemRepository.count();
        assertEquals(24, recipeCount, "Počet položek receptur v databázi nesouhlasí (4 produkty x 6 surovin)");
    }

    @Test
    void seedAttendance_DatabaseIsPopulated() {
        long attendanceCount = attendanceRecordRepository.count();
        assertTrue(attendanceCount >= 30, "Historie docházky by měla obsahovat desítky záznamů (3 pracovníci x 3 dny x 4 záznamy)");
    }

    @Test
    void seedOrders_DatabaseIsPopulated() {
        List<Order> orders = orderRepository.findAll();
        assertEquals(6, orders.size(), "Celkový počet objednávek by měl být 6");
        if (orders.isEmpty()) return;

        Order firstOrder = orderRepository.findById(orders.get(0).getId()).orElseThrow();
        assertTrue(!firstOrder.getItems().isEmpty(), "Objednávka by měla mít položky (OrderItem)");
        assertTrue(!firstOrder.getStatusHistory().isEmpty(), "Objednávka by měla mít vygenerovanou počáteční historii stavu");
        assertTrue(firstOrder.getTotalAmount().doubleValue() >= 150.0, "Celková hodnota objednávky by měla obsahovat alespoň cenu dopravy");

        boolean hasOrderVariants = orders.stream()
                .flatMap(o -> o.getItems().stream())
                .anyMatch(item -> item.getSelectedLazure() != null);
        assertTrue(hasOrderVariants, "Seeder by měl vytvořit alespoň jednu položku objednávky s vybranou lazurou.");
    }
}