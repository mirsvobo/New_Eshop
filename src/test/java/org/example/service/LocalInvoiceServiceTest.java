package org.example.service;

import org.example.model.Order;
import org.example.model.OrderItem;
import org.example.model.Product;
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
import java.util.List;

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
    void generateHtmlInvoice_PassesCorrectContext() {
        TaxRate tax21 = new TaxRate(1L, "DPH 21%", new BigDecimal("21.00"), true, false);

        Product product = Product.builder()
                .name("Test Produkt")
                .taxRate(tax21)
                .build();

        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(2)
                .unitPrice(new BigDecimal("121.00"))
                .build();

        Order order = new Order();
        order.setId(1L);
        order.setOrderNumber(TEST_ORDER_NUMBER);
        order.setItems(List.of(item));
        order.setShippingCost(BigDecimal.ZERO);
        order.setDiscountAmount(BigDecimal.ZERO);
        order.setTotalAmount(new BigDecimal("242.00"));
        order.setDeliveryAddress("Test Ulice 123");

        when(templateEngine.process(anyString(), any(Context.class))).thenReturn("<html>Test Faktura</html>");

        localInvoiceService.generateHtmlInvoice(order);

        ArgumentCaptor<Context> contextCaptor = ArgumentCaptor.forClass(Context.class);
        verify(templateEngine).process(eq("faktura-sablona"), contextCaptor.capture());

        Context context = contextCaptor.getValue();
        Order capturedOrder = (Order) context.getVariable("order");

        @SuppressWarnings("unchecked")
        java.util.Map<BigDecimal, java.util.Map<String, BigDecimal>> capturedTax =
                (java.util.Map<BigDecimal, java.util.Map<String, BigDecimal>>) context.getVariable("taxSummary");

        assertNotNull(capturedOrder, "Kontext by měl obsahovat objekt order");
        assertEquals(TEST_ORDER_NUMBER, capturedOrder.getOrderNumber(), "Objednávka v kontextu by měla odpovídat té testované");
        assertNotNull(capturedTax, "Kontext by měl obsahovat vygenerovanou strukturu pro DPH (taxSummary)");
        assertTrue(capturedTax.containsKey(new BigDecimal("21.00")), "Měla by být zachycena daň 21%");
    }
}