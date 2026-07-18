package org.example.service;

import org.example.model.Order;
import org.example.model.OrderItem;
import org.example.model.Product;
import org.example.model.TaxMode;
import org.example.model.TaxRate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LocalInvoiceServiceTest {

    @Mock
    private TemplateEngine templateEngine;

    @InjectMocks
    private LocalInvoiceService localInvoiceService;

    private final String TEST_ORDER_NUMBER = "TEST-INV-123";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(localInvoiceService, "baseStorageDir", "local-storage/");
    }

    @AfterEach
    void cleanUp() {
        try {
            Files.deleteIfExists(Paths.get("local-storage/invoices/FAKTURA-" + TEST_ORDER_NUMBER + ".html"));
        } catch (Exception ignored) {
        }
    }

    @Test
    void generateHtmlInvoice_StandardMode_Calculates21PercentTax() {
        TaxRate tax21 = new TaxRate(1L, "DPH 21%", new BigDecimal("21.00"), true, false);
        Product product = Product.builder().name("Test Produkt").taxRate(tax21).build();
        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("121.00"))
                .actualTaxRate(new BigDecimal("21.00"))
                .build();

        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber(TEST_ORDER_NUMBER);
        order.setItems(List.of(item));
        order.setTaxMode(TaxMode.STANDARD);
        order.setShippingCost(new BigDecimal("150.00"));
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("392.00"));
        order.setDeliveryAddress("Test Ulice 123");

        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test Faktura</html>");

        localInvoiceService.generateHtmlInvoice(order);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("faktura-sablona"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        Order capturedOrder = (Order) context.getVariable("order");

        @SuppressWarnings("unchecked")
        Map<BigDecimal, Map<String, BigDecimal>> capturedTax =
                (Map<BigDecimal, Map<String, BigDecimal>>) context.getVariable("taxSummary");

        assertNotNull(capturedOrder);
        assertTrue(capturedTax.containsKey(new BigDecimal("21.00")), "Měla by být zachycena daň 21%");
        assertFalse(capturedTax.containsKey(new BigDecimal("12.00")), "Daň 12% by se neměla vyskytovat");
    }

    @Test
    void generateHtmlInvoice_ReducedMode_Calculates12PercentTax() {
        // Produkt má standardně v katalogu 21 %
        TaxRate tax21 = new TaxRate(1L, "DPH 21%", new BigDecimal("21.00"), true, false);
        Product product = Product.builder().name("Sada podtácků").taxRate(tax21).build();

        // Ale položka objednávky si správně drží 12 %
        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(1)
                .unitPrice(new BigDecimal("112.00"))
                .actualTaxRate(new BigDecimal("12.00"))
                .build();

        Order order = new Order();
        order.setId(2L);
        order.setOrderNumber(TEST_ORDER_NUMBER);
        order.setItems(List.of(item));
        order.setTaxMode(TaxMode.REDUCED);
        order.setShippingCost(new BigDecimal("150.00")); // Doprava bude též daněna 12%
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("262.00"));

        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test Faktura Snížená</html>");

        localInvoiceService.generateHtmlInvoice(order);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("faktura-sablona"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        @SuppressWarnings("unchecked")
        Map<BigDecimal, Map<String, BigDecimal>> capturedTax =
                (Map<BigDecimal, Map<String, BigDecimal>>) context.getVariable("taxSummary");

        assertNotNull(capturedTax);
        assertTrue(capturedTax.containsKey(new BigDecimal("12.00")), "Musí se spočítat 12% DPH z položky i dopravy");
        assertFalse(capturedTax.containsKey(new BigDecimal("21.00")), "Původní 21% DPH (z entity Product) musí být ignorována");
    }
    @Test
    void exportInvoiceToExcel_GeneratesValidExcel() throws Exception {
        org.example.model.Order order = new org.example.model.Order();
        order.setOrderNumber("INV-123");
        order.setCreatedAt(java.time.LocalDateTime.now());
        order.setTotalAmount(new java.math.BigDecimal("1210.00"));
        order.setShippingCost(new java.math.BigDecimal("150.00"));
        order.setTaxMode(org.example.model.TaxMode.STANDARD);
        order.setCustomer(org.example.model.User.builder().firstName("Pepa").lastName("Zdepa").build());

        org.example.model.Product p = org.example.model.Product.builder().name("Stul").unit("ks").build();
        org.example.model.OrderItem item = org.example.model.OrderItem.builder()
                .product(p)
                .quantity(1)
                .unitPrice(new java.math.BigDecimal("1000"))
                .actualTaxRate(new java.math.BigDecimal("21"))
                .build();
        order.setItems(java.util.List.of(item));

        java.io.ByteArrayInputStream bais = localInvoiceService.exportInvoiceToExcel(order);
        org.junit.jupiter.api.Assertions.assertNotNull(bais);

        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(bais)) {
            org.junit.jupiter.api.Assertions.assertEquals("Faktura_INV-123", workbook.getSheetAt(0).getSheetName());
            org.junit.jupiter.api.Assertions.assertNotNull(workbook.getSheetAt(0).getRow(0).getCell(0));
        }
    }

    @Test
    void exportOrdersToExcel_GeneratesValidExcel() throws Exception {
        org.example.model.Order order = new org.example.model.Order();
        order.setOrderNumber("INV-123");
        order.setCreatedAt(java.time.LocalDateTime.now());
        order.setTotalAmount(new java.math.BigDecimal("1210.00"));
        order.setShippingCost(new java.math.BigDecimal("150.00"));
        order.setCustomer(org.example.model.User.builder().firstName("Pepa").lastName("Zdepa").build());

        java.io.ByteArrayInputStream bais = localInvoiceService.exportOrdersToExcel(java.util.List.of(order));
        org.junit.jupiter.api.Assertions.assertNotNull(bais);

        try (org.apache.poi.ss.usermodel.Workbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(bais)) {
            org.junit.jupiter.api.Assertions.assertEquals("Přehled Objednávek", workbook.getSheetAt(0).getSheetName());
            org.junit.jupiter.api.Assertions.assertEquals("Číslo obj.", workbook.getSheetAt(0).getRow(0).getCell(0).getStringCellValue());
        }
    }
}