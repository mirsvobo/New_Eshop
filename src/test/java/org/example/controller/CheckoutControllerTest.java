package org.example.controller;

import org.example.dto.CartItemDto;
import org.example.model.Order;
import org.example.model.TaxMode;
import org.example.service.Cart;
import org.example.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Cart cart;

    @MockitoBean
    private OrderService orderService;

    @BeforeEach
    void setUp() {
        // Namockujeme chování košíku pro všechny metody v tomto testu,
        // aby nespadlo renderování šablony checkout.html v Thymeleafu.
        CartItemDto item = CartItemDto.builder()
                .productId(1L)
                .price(BigDecimal.TEN)
                .basePrice(BigDecimal.TEN)
                .quantity(1)
                .build();

        when(cart.getItems()).thenReturn(List.of(item));
        when(cart.getTotalPrice()).thenReturn(BigDecimal.TEN);
        when(cart.getTaxMode()).thenReturn(TaxMode.STANDARD);

        // Namockujeme chování OrderService, abychom zamezili zásahům do databáze během testování kontroleru
        Order mockedOrder = new Order();
        mockedOrder.setOrderNumber("ORD-12345");
        when(orderService.processCheckout(any(), any(), any())).thenReturn(mockedOrder);
    }

    @Test
    void checkoutGet_LoadsForm() throws Exception {
        mockMvc.perform(get("/kosik/pokladna"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("checkoutForm"))
                .andExpect(view().name("checkout"));
    }

    @Test
    void checkoutPost_ReducedMode_WithoutAffidavit_ReturnsErrors() throws Exception {
        mockMvc.perform(post("/kosik/objednat")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Novák")
                        .param("email", "jan@test.cz")
                        .param("phone", "123456789")
                        .param("billingStreet", "Testovací 1")
                        .param("billingCity", "Praha")
                        .param("billingZipCode", "11100")
                        .param("taxMode", "REDUCED")
                        .param("affidavitSigned", "false"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attributeHasFieldErrors("checkoutForm", "affidavitSigned"));
    }

    @Test
    void checkoutPost_ReducedMode_WithIco_ReturnsErrors() throws Exception {
        mockMvc.perform(post("/kosik/objednat")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Novák")
                        .param("email", "jan@test.cz")
                        .param("phone", "123456789")
                        .param("billingStreet", "Testovací 1")
                        .param("billingCity", "Praha")
                        .param("billingZipCode", "11100")
                        .param("taxMode", "REDUCED")
                        .param("affidavitSigned", "true")
                        .param("ico", "12345678")
                        .param("dic", "CZ12345678"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"))
                .andExpect(model().attributeHasFieldErrors("checkoutForm", "ico"));
    }

    @Test
    void checkoutPost_StandardMode_WithIco_IsOk() throws Exception {
        // V případě Standard režimu je IČO povolené a affidavitSigned není nutné
        mockMvc.perform(post("/kosik/objednat")
                        .with(csrf())
                        .param("firstName", "Jan")
                        .param("lastName", "Novák")
                        .param("email", "jan@test.cz")
                        .param("phone", "123456789")
                        .param("billingStreet", "Testovací 1")
                        .param("billingCity", "Praha")
                        .param("billingZipCode", "11100")
                        .param("taxMode", "STANDARD")
                        .param("affidavitSigned", "false")
                        .param("ico", "12345678")
                        .param("dic", "CZ12345678"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kosik/potvrzeni/ORD-12345"));
    }
    @Test
    void confirmation_ReturnsView() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/kosik/potvrzeni/ORD-123"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.view().name("potvrzeni"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.model().attributeExists("orderNumber"));
    }
}