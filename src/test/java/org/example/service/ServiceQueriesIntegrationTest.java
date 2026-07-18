package org.example.service;

import org.example.model.*;
import org.example.repository.*;
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
class ServiceQueriesIntegrationTest {

    @Autowired
    private AttendanceService attendanceService;

    @Autowired
    private InventoryService inventoryService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private AttendanceRecordRepository attendanceRecordRepository;

    @Autowired
    private StockMovementRepository stockMovementRepository;

    @Autowired
    private TaxRateRepository taxRateRepository;

    @Test
    void testAttendanceFilteredRecords_Integration() {
        User user = userRepository.save(User.builder()
                .email("audit-test@test.cz")
                .password("pass")
                .firstName("Test")
                .lastName("Testovic")
                .role(User.Role.ROLE_EMPLOYEE)
                .active(true)
                .build());

        attendanceRecordRepository.save(AttendanceRecord.builder()
                .employee(user)
                .type(AttendanceRecord.AttendanceType.CLOCK_IN)
                .timestamp(LocalDateTime.now())
                .build());

        // Testujeme prázdné filtry (mělo by vrátit vše)
        List<AttendanceRecord> all = attendanceService.getFilteredRecords(null, null, null);
        assertNotNull(all);
        assertFalse(all.isEmpty());

        // Testujeme všechny filtry zapnuté
        List<AttendanceRecord> filtered = attendanceService.getFilteredRecords(
                user.getId(),
                AttendanceRecord.AttendanceType.CLOCK_IN,
                "today"
        );
        assertFalse(filtered.isEmpty());

        // Pokrytí dalších "switch" větví pro dateRange
        attendanceService.getFilteredRecords(null, null, "wtd");
        attendanceService.getFilteredRecords(null, null, "mtd");
        attendanceService.getFilteredRecords(null, null, "ytd");
    }

    @Test
    void testInventoryFilteredMovements_Integration() {
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

        Product p = productRepository.save(Product.builder()
                .name("Zkušební prkno")
                .price(new BigDecimal("100"))
                .taxRate(tax)
                .stockQuantity(10)
                .type(Product.ProductType.PRODUCT)
                .unit("ks")
                .active(true)
                .build());

        stockMovementRepository.save(StockMovement.builder()
                .product(p)
                .performedBy(user)
                .quantity(5)
                .type(StockMovement.MovementType.RECEIPT)
                .timestamp(LocalDateTime.now())
                .build());

        // Test prázdných filtrů
        List<StockMovement> all = inventoryService.getFilteredMovements(null, null, null, null, null, null);
        assertNotNull(all);
        assertFalse(all.isEmpty());

        // Test pozitivní direction a min/max limitů
        List<StockMovement> filteredPos = inventoryService.getFilteredMovements(
                p.getId(), user.getId(), "positive", 1.0, 10.0, "today"
        );
        assertFalse(filteredPos.isEmpty());

        // Pokrytí negativní direction a ostatních variant dateRange
        inventoryService.getFilteredMovements(null, null, "negative", null, null, "week");
        inventoryService.getFilteredMovements(null, null, null, null, null, "month");
        inventoryService.getFilteredMovements(null, null, null, null, null, "year");
    }
}