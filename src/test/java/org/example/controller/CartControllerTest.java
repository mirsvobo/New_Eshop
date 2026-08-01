package org.example.controller;

import org.example.dto.CartItemDto;
import org.example.model.LayerType;
import org.example.model.Product;
import org.example.model.TaxMode;
import org.example.repository.ProductRepository;
import org.example.service.Cart;
import org.example.service.ProductImageLayerService;
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
import static org.mockito.Mockito.*;
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

    @MockitoBean
    private ProductImageLayerService productImageLayerService;

    @BeforeEach
    void setUp() {
        when(cart.getTaxMode()).thenReturn(TaxMode.STANDARD);
        when(cart.getTotalItems()).thenReturn(0);
    }

    @Test
    @WithMockUser
    void addToCart_WithValidLazure_MapsValidatedValueToCartItem() throws Exception {
        Product mockProduct = configurableProduct();
        when(productRepository.findById(1L)).thenReturn(Optional.of(mockProduct));
        when(productImageLayerService.validateAndResolveSelection(1L, LayerType.LAZURE, "Pinie"))
                .thenReturn("Pinie");
        when(productImageLayerService.validateAndResolveSelection(1L, LayerType.ROOF_COLOR, null))
                .thenReturn(null);

        mockMvc.perform(post("/kosik/pridat")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "1")
                        .param("selectedLazure", "Pinie"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kosik"));

        ArgumentCaptor<CartItemDto> captor = ArgumentCaptor.forClass(CartItemDto.class);
        verify(cart).addItem(captor.capture());

        CartItemDto addedItem = captor.getValue();
        assertEquals("Pinie", addedItem.getSelectedLazure(), "Lazura se z requestu nepředala do DTO košíku.");
        assertEquals(null, addedItem.getSelectedRoofColor(), "Barva střechy má zůstat prázdná.");
        assertEquals("/images/altan.jpg", addedItem.getImageUrl(), "Obrázek se z produktu nepředal do DTO košíku.");
    }

    @Test
    @WithMockUser
    void addToCart_WithInvalidLazure_RejectsRequestWithCzechMessage() throws Exception {
        when(productRepository.findById(1L)).thenReturn(Optional.of(configurableProduct()));
        when(productImageLayerService.validateAndResolveSelection(1L, LayerType.LAZURE, "Neplatná"))
                .thenThrow(new IllegalArgumentException("Vybraná lazura není pro tento produkt dostupná."));

        mockMvc.perform(post("/kosik/pridat")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "1")
                        .param("selectedLazure", "Neplatná"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produkty/1"))
                .andExpect(flash().attribute("error", "Vybraná lazura není pro tento produkt dostupná."));

        verify(cart, never()).addItem(any(CartItemDto.class));
    }

    @Test
    @WithMockUser
    void addToCart_WithValidRoofColor_MapsValidatedValueToCartItem() throws Exception {
        when(productRepository.findById(1L)).thenReturn(Optional.of(configurableProduct()));
        when(productImageLayerService.validateAndResolveSelection(1L, LayerType.LAZURE, null))
                .thenReturn(null);
        when(productImageLayerService.validateAndResolveSelection(1L, LayerType.ROOF_COLOR, "Antracit"))
                .thenReturn("Antracit");

        mockMvc.perform(post("/kosik/pridat")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "1")
                        .param("selectedRoofColor", "Antracit"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kosik"));

        ArgumentCaptor<CartItemDto> captor = ArgumentCaptor.forClass(CartItemDto.class);
        verify(cart).addItem(captor.capture());
        assertEquals("Antracit", captor.getValue().getSelectedRoofColor());
    }

    @Test
    @WithMockUser
    void addToCart_WithInvalidRoofColor_RejectsRequestWithCzechMessage() throws Exception {
        when(productRepository.findById(1L)).thenReturn(Optional.of(configurableProduct()));
        when(productImageLayerService.validateAndResolveSelection(1L, LayerType.LAZURE, null))
                .thenReturn(null);
        when(productImageLayerService.validateAndResolveSelection(1L, LayerType.ROOF_COLOR, "Neplatná"))
                .thenThrow(new IllegalArgumentException("Vybraná barva střechy není pro tento produkt dostupná."));

        mockMvc.perform(post("/kosik/pridat")
                        .with(csrf())
                        .param("productId", "1")
                        .param("quantity", "1")
                        .param("selectedRoofColor", "Neplatná"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produkty/1"))
                .andExpect(flash().attribute(
                        "error",
                        "Vybraná barva střechy není pro tento produkt dostupná."
                ));

        verify(cart, never()).addItem(any(CartItemDto.class));
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

    private Product configurableProduct() {
        return Product.builder()
                .id(1L)
                .name("Konfigurovatelný altán")
                .price(new BigDecimal("15000"))
                .stockQuantity(2)
                .imageUrl("altan.jpg")
                .build();
    }
}
