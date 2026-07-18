package org.example.controller;

import org.example.repository.OrderRepository;
import org.example.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AdminControllerTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private OrderStatusService orderStatusService;
    @Mock
    private ProductService productService;
    @Mock
    private UserService userService;
    @Mock
    private AuditService auditService;
    @Mock
    private LocalInvoiceService localInvoiceService;
    @Mock
    private CouponService couponService;
    @Mock
    private TaxRateService taxRateService;

    @Mock
    private Model model;

    @InjectMocks
    private AdminController adminController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void dashboard_ShouldReturnDashboardView() {
        when(orderRepository.count()).thenReturn(10L);
        when(productService.count()).thenReturn(5L);
        when(orderRepository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(Collections.emptyList()));
        when(orderRepository.sumTotalRevenue()).thenReturn(BigDecimal.ZERO);
        when(userService.count()).thenReturn(2L);
        when(couponService.countActive()).thenReturn(1L);
        when(taxRateService.count()).thenReturn(3L);

        String viewName = adminController.dashboard(model);

        assertEquals("admin/dashboard", viewName);
        verify(model).addAttribute(eq("totalOrders"), anyLong());
    }
    @Test
    void orderList_ShouldReturnOrderListView() {
        when(orderRepository.findAll(any(Sort.class))).thenReturn(Collections.emptyList());

        String viewName = adminController.orderList(model);

        assertEquals("admin/objednavky", viewName);
        verify(model).addAttribute(eq("orders"), any());
    }

    @Test
    void orderDetail_ShouldReturnOrderDetailView() {
        org.example.model.Order order = new org.example.model.Order();
        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        when(orderStatusService.getAllOrdered()).thenReturn(Collections.emptyList());

        String viewName = adminController.orderDetail(1L, model);

        assertEquals("admin/objednavka-detail", viewName);
        verify(model).addAttribute("order", order);
        verify(model).addAttribute(eq("allStatuses"), any());
    }

    @Test
    void updateOrderStatus_ShouldRedirectAndLog() {
        org.example.model.Order order = new org.example.model.Order();
        order.setOrderNumber("ORD-123");
        order.setStatusHistory(new java.util.ArrayList<>());

        org.example.model.OrderStatus status = new org.example.model.OrderStatus();
        status.setName("Odesláno");

        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        when(orderStatusService.findById(2L)).thenReturn(status);

        java.security.Principal principal = mock(java.security.Principal.class);
        when(principal.getName()).thenReturn("admin@test.cz");

        org.example.model.User user = new org.example.model.User();
        when(userService.findByEmail("admin@test.cz")).thenReturn(java.util.Optional.of(user));

        org.springframework.web.servlet.mvc.support.RedirectAttributes ra = mock(org.springframework.web.servlet.mvc.support.RedirectAttributes.class);

        String viewName = adminController.updateOrderStatus(1L, 2L, "Zkušební poznámka", principal, ra);

        assertEquals("redirect:/admin/objednavky/1", viewName);
        assertEquals(status, order.getStatus());
        assertFalse(order.getStatusHistory().isEmpty());
        verify(orderRepository).save(order);
        verify(auditService).log(eq("OBJEDNÁVKY"), eq("ZMENA_STAVU"), anyString());
        verify(ra).addFlashAttribute(eq("success"), anyString());
    }

    @Test
    void auditLogs_ShouldReturnAuditLogsView() {
        when(auditService.getFilteredLogs(1L, "PRODUKTY")).thenReturn(Collections.emptyList());
        when(userService.findAll()).thenReturn(Collections.emptyList());
        when(auditService.getAllModules()).thenReturn(Collections.emptyList());

        String viewName = adminController.auditLogs(1L, "PRODUKTY", model);

        assertEquals("admin/audit-logs", viewName);
        verify(model).addAttribute(eq("logs"), any());
        verify(model).addAttribute("selectedUserId", 1L);
        verify(model).addAttribute("selectedModule", "PRODUKTY");
    }
    @Test
    void exportInvoiceExcel_ShouldReturnFile() throws Exception {
        org.example.model.Order order = new org.example.model.Order();
        order.setOrderNumber("123");

        when(orderRepository.findById(1L)).thenReturn(java.util.Optional.of(order));
        when(localInvoiceService.exportInvoiceToExcel(order)).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));

        org.springframework.http.ResponseEntity<org.springframework.core.io.InputStreamResource> response = adminController.exportInvoiceExcel(1L);

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getHeaders().containsKey(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION));
    }

    @Test
    void exportAllOrdersExcel_ShouldReturnFile() throws Exception {
        when(orderRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(java.util.Collections.emptyList());
        when(localInvoiceService.exportOrdersToExcel(anyList())).thenReturn(new java.io.ByteArrayInputStream(new byte[0]));

        org.springframework.http.ResponseEntity<org.springframework.core.io.InputStreamResource> response = adminController.exportAllOrdersExcel();

        assertEquals(200, response.getStatusCodeValue());
        assertTrue(response.getHeaders().containsKey(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION));
    }
}