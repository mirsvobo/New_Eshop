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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
                .imageUrl("altan.jpg") // Přidáno pro test obrázku
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
        assertEquals("/images/altan.jpg", addedItem.getImageUrl(), "Obrázek se z produktu nepředal do DTO košíku.");
    }
    @Test
    @WithMockUser
    void viewCart_ReturnsCartView() throws Exception {
        when(cart.getTotalPrice()).thenReturn(BigDecimal.ZERO);
        when(cart.getDiscountAmount()).thenReturn(BigDecimal.ZERO);
        when(cart.getFinalPrice()).thenReturn(BigDecimal.ZERO);

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/kosik"))
                .andExpect(status().isOk())
                .andExpect(view().name("kosik"))
                .andExpect(model().attributeExists("cartItems"));
    }

    @Test
    @WithMockUser
    void updateCartItem_RedirectsToCart() throws Exception {
        mockMvc.perform(post("/kosik/upravit")
                        .param("productId", "1")
                        .param("quantity", "5")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kosik"));

        verify(cart).updateQuantity(1L, 5);
    }

    @Test
    @WithMockUser
    void removeFromCart_RedirectsToCart() throws Exception {
        mockMvc.perform(post("/kosik/odstranit")
                        .param("productId", "1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kosik"));
    }

    @Test
    @WithMockUser
    void clearCart_RedirectsToCart() throws Exception {
        mockMvc.perform(post("/kosik/vycistit")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kosik"));

        verify(cart).clear();
    }

    @Test
    @WithMockUser
    void switchTaxMode_RedirectsToCart() throws Exception {
        mockMvc.perform(post("/kosik/rezim")
                        .param("taxMode", "REDUCED")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kosik"));

        verify(cart).setTaxMode(TaxMode.REDUCED);
    }
}