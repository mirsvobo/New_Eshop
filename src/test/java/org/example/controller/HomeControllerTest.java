package org.example.controller;

import org.example.dto.CartItemDto;
import org.example.service.Cart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Cart cart;

    @Test
    void homePage_LoadsSuccessfully() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void checkoutPage_LoadsSuccessfully_WithItemsInCart() throws Exception {
        CartItemDto item = CartItemDto.builder()
                .productId(1L)
                .productName("Testovací produkt")
                .quantity(1)
                .price(new BigDecimal("100.00"))
                .basePrice(new BigDecimal("82.64"))
                .taxRateValue(new BigDecimal("21.0"))
                .build();

        when(cart.getItems()).thenReturn(List.of(item));
        when(cart.getTotalPrice()).thenReturn(new BigDecimal("100.00"));

        mockMvc.perform(get("/kosik/pokladna"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"));
    }
}