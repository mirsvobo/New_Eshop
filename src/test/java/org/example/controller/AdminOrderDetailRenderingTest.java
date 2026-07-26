package org.example.controller;

import org.example.model.*;
import org.example.repository.InstallationPostRepository;
import org.example.repository.OrderRepository;
import org.example.service.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request
        .MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result
        .MockMvcResultMatchers.status;

@WebMvcTest(AdminController.class)
class AdminOrderDetailRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean
    private InstallationPostRepository
            installationPostRepository;

    @MockitoBean
    private OrderStatusService orderStatusService;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean
    private LocalInvoiceService localInvoiceService;

    @MockitoBean
    private CouponService couponService;

    @MockitoBean
    private TaxRateService taxRateService;

    @MockitoBean(name = "cart")
    private Cart cart;

    @BeforeEach
    void setUp() {
        when(cart.getTaxMode())
                .thenReturn(TaxMode.STANDARD);

        when(cart.getTotalItems())
                .thenReturn(0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void orderDetail_WithReducedTaxMode_Shows12PercentTaxInHtml()
            throws Exception {
        TaxRate originalTax = TaxRate.builder()
                .name("Základní sazba 21%")
                .rate(new BigDecimal("21"))
                .build();

        Product product = Product.builder()
                .name("Sada podtácků")
                .unit("ks")
                .taxRate(originalTax)
                .build();

        OrderItem item = OrderItem.builder()
                .product(product)
                .quantity(3)
                .unitPrice(new BigDecimal("616.00"))
                .actualTaxRate(
                        new BigDecimal("12.00")
                )
                .build();

        OrderStatus status = OrderStatus.builder()
                .name("Nová")
                .colorClass("bg-blue")
                .build();

        Order order = Order.builder()
                .id(1L)
                .orderNumber("ORD-123")
                .customer(
                        User.builder()
                                .firstName("Jan")
                                .lastName("Novák")
                                .build()
                )
                .billingAddress("Adresa")
                .deliveryAddress("Adresa")
                .totalAmount(
                        new BigDecimal("1848.00")
                )
                .shippingCost(
                        new BigDecimal("150.00")
                )
                .taxMode(TaxMode.REDUCED)
                .status(status)
                .createdAt(LocalDateTime.now())
                .items(List.of(item))
                .build();

        given(orderRepository.findById(1L))
                .willReturn(Optional.of(order));

        mockMvc.perform(
                        get("/admin/objednavky/1")
                )
                .andExpect(status().isOk())
                .andExpect(
                        content().string(
                                containsString(
                                        "Snížená sazba (12 % DPH)"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                not(
                                        containsString(
                                                "Standardní sazba (21 %DPH)"
                                        )
                                )
                        )
                );
    }
}