package org.example.controller;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.TaxMode;
import org.example.model.User;
import org.example.repository.OrderRepository;
import org.example.repository.UserRepository;
import org.example.service.Cart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(CustomerController.class)
class CustomerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean(name = "cart")
    private Cart cart;

    @Test
    @WithMockUser(username = "zakaznik@test.cz", roles = "CUSTOMER")
    void myAccount_ShouldReturnView() throws Exception {
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("zakaznik@test.cz");

        when(userRepository.findByEmail("zakaznik@test.cz")).thenReturn(Optional.of(customer));
        when(orderRepository.findByCustomerOrderByCreatedAtDesc(customer)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/muj-ucet"))
                .andExpect(status().isOk())
                .andExpect(view().name("muj-ucet"))
                .andExpect(model().attributeExists("user"))
                .andExpect(model().attributeExists("orders"));
    }

    @Test
    @WithMockUser(username = "zakaznik@test.cz", roles = "CUSTOMER")
    void customerOrderDetail_ValidOrder_ShouldReturnView() throws Exception {
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("zakaznik@test.cz");

        OrderStatus status = new OrderStatus();
        status.setName("Nová");
        status.setColorClass("bg-blue-100");

        Order order = new Order();
        order.setId(10L);
        order.setOrderNumber("ORD-TEST-123");
        order.setCustomer(customer);
        order.setStatus(status);
        order.setCreatedAt(LocalDateTime.now());
        order.setDeliveryAddress("Testovací adresa");
        order.setBillingAddress("Testovací adresa");
        order.setTotalAmount(new BigDecimal("1500.00"));
        order.setShippingCost(new BigDecimal("150.00"));
        order.setTaxMode(TaxMode.STANDARD);
        order.setItems(Collections.emptyList());
        order.setStatusHistory(Collections.emptyList());

        when(userRepository.findByEmail("zakaznik@test.cz")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/muj-ucet/objednavka/10"))
                .andExpect(status().isOk())
                .andExpect(view().name("objednavka-detail-zakaznik"))
                .andExpect(model().attributeExists("order"));
    }

    @Test
    @WithMockUser(username = "zakaznik@test.cz", roles = "CUSTOMER")
    void customerOrderDetail_ForeignOrder_ShouldRedirect() throws Exception {
        User customer = new User();
        customer.setId(1L);
        customer.setEmail("zakaznik@test.cz");

        User otherCustomer = new User();
        otherCustomer.setId(2L);

        Order order = new Order();
        order.setId(10L);
        order.setCustomer(otherCustomer);

        when(userRepository.findByEmail("zakaznik@test.cz")).thenReturn(Optional.of(customer));
        when(orderRepository.findById(10L)).thenReturn(Optional.of(order));

        mockMvc.perform(get("/muj-ucet/objednavka/10"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/muj-ucet"));
    }
}