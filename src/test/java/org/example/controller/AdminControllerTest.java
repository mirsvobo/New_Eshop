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
import org.springframework.ui.Model;

import java.math.BigDecimal;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}