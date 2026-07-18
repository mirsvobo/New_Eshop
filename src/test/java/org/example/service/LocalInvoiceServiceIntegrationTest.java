package org.example.service;

import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.model.Order;
import org.example.model.OrderItem;
import org.example.model.Product;
import org.example.model.TaxMode;
import org.example.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class LocalInvoiceServiceIntegrationTest {

    @Autowired
    private LocalInvoiceService localInvoiceService;

    @Test
    void exportInvoiceToExcel_CreatesValidWorkbook() throws Exception {
        User customer = User.builder().firstName("Jan").lastName("Novák").build();
        Product product = Product.builder().name("Testovací Stůl").unit("ks").build();

        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("5000.00"))
                .actualTaxRate(new BigDecimal("21.00"))
                .build();

        Order order = Order.builder()
                .orderNumber("INV-999")
                .createdAt(LocalDateTime.now())
                .customer(customer)
                .billingAddress("Fakturační Adresa 1")
                .totalAmount(new BigDecimal("10150.00"))
                .shippingCost(new BigDecimal("150.00"))
                .taxMode(TaxMode.STANDARD)
                .items(List.of(item))
                .build();

        ByteArrayInputStream inputStream = localInvoiceService.exportInvoiceToExcel(order);
        assertNotNull(inputStream, "Výstupní stream nesmí být null");

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals("Faktura_INV-999", workbook.getSheetAt(0).getSheetName());
            assertEquals("Faktura / Daňový doklad: INV-999", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
        }
    }

    @Test
    void exportOrdersToExcel_CreatesValidWorkbook() throws Exception {
        User customer = User.builder().firstName("Petr").lastName("Dvořák").build();

        Order order = Order.builder()
                .orderNumber("ORD-888")
                .createdAt(LocalDateTime.now())
                .customer(customer)
                .billingAddress("Fakturační 2")
                .totalAmount(new BigDecimal("2500.00"))
                .shippingCost(new BigDecimal("100.00"))
                .taxMode(TaxMode.REDUCED)
                .build();

        ByteArrayInputStream inputStream = localInvoiceService.exportOrdersToExcel(List.of(order));
        assertNotNull(inputStream, "Výstupní stream nesmí být null");

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            assertEquals("Přehled Objednávek", workbook.getSheetAt(0).getSheetName());
            assertEquals("Číslo obj.", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
            assertEquals("ORD-888", workbook.getSheetAt(0).getRow(1).getCell(0).getStringCellValue());
        }
    }
}