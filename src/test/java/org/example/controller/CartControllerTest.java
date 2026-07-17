package org.example.controller;

import org.example.dto.CartItemDto;
import org.example.model.Product;
import org.example.model.TaxMode;
import org.example.repository.ProductRepository;
import org.example.service.Cart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CartController.class)
class CartControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "cart")
    private Cart cart;

    @MockitoBean
    private ProductRepository productRepository;

    @BeforeEach
    void setUp() {
        when(cart.getTaxMode()).thenReturn(TaxMode.STANDARD);
        when(cart.getTotalItems()).thenReturn(0);
    }

    @Test
    @WithMockUser
    void addToCart_WithConfigurators_MapsCorrectlyToCartItem() throws Exception {
        // Arrange
        Product mockProduct = Product.builder()
                .id(1L)
                .name("Konfigurovatelný Altán")
                .price(new BigDecimal("15000"))
                .stockQuantity(2)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));

        // Act
        mockMvc.perform(post("/kosik/pridat")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "1")
                        .param("selectedLazure", "Pinie")
                        .param("selectedRoofColor", "Černá")
                        .param("selectedDesign", "Moderní"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kosik"));

        // Assert
        ArgumentCaptor<CartItemDto> captor = ArgumentCaptor.forClass(CartItemDto.class);
        verify(cart).addItem(captor.capture());

        CartItemDto addedItem = captor.getValue();
        assertEquals("Pinie", addedItem.getSelectedLazure(), "Lazura se z requestu nepředala do DTO košíku.");
        assertEquals("Černá", addedItem.getSelectedRoofColor(), "Barva střechy se z requestu nepředala do DTO košíku.");
        assertEquals("Moderní", addedItem.getSelectedDesign(), "Design se z requestu nepředal do DTO košíku.");
    }
}