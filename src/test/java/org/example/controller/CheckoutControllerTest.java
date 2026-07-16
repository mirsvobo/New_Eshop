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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
class CheckoutControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private Cart cart;

    @Test
    void checkoutGet_LoadsForm() throws Exception {
        CartItemDto item = CartItemDto.builder()
                .price(BigDecimal.TEN)
                .quantity(1)
                .build();
        when(cart.getItems()).thenReturn(List.of(item));
        when(cart.getTotalPrice()).thenReturn(BigDecimal.TEN);

        mockMvc.perform(get("/kosik/pokladna"))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("checkoutForm"))
                .andExpect(view().name("checkout"));
    }
}