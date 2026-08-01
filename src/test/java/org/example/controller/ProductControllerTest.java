package org.example.controller;

import org.example.model.Product;
import org.example.model.TaxMode;
import org.example.model.TaxRate;
import org.example.repository.ProductRepository;
import org.example.service.Cart;
import org.example.service.ProductImageLayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean
    private ProductImageLayerService productImageLayerService;

    @MockitoBean(name = "cart")
    private Cart cart;

    @BeforeEach
    void setUp() {
        when(cart.getTaxMode()).thenReturn(TaxMode.STANDARD);
        when(cart.getTotalItems()).thenReturn(0);
    }

    @Test
    @WithMockUser
    void shouldReturnActiveProductsSortedByPrice() throws Exception {
        given(productRepository.findByActiveTrueAndType(eq(Product.ProductType.PRODUCT), any(Sort.class)))
                .willReturn(Collections.emptyList());

        mockMvc.perform(get("/produkty").param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(view().name("produkty"))
                .andExpect(model().attribute("currentSort", "price_asc"));
    }

    @Test
    @WithMockUser
    void shouldShowProductDetailWhenActiveAndTypeIsProduct() throws Exception {
        TaxRate standardTax = TaxRate.builder().rate(new BigDecimal("21.0")).build();

        Product product = Product.builder()
                .id(1L)
                .name("Pracovní Stůl")
                .active(true)
                .type(Product.ProductType.PRODUCT)
                .price(new BigDecimal("1000.00"))
                .stockQuantity(10.0)
                .unit("ks")
                .taxRate(standardTax)
                .build();

        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(productImageLayerService.getActiveLayersForProduct(1L)).willReturn(Collections.emptyList());

        mockMvc.perform(get("/produkty/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("produkt-detail"))
                .andExpect(model().attribute("product", product))
                .andExpect(model().attributeExists("lazureLayers", "roofColorLayers"));
    }

    @Test
    @WithMockUser
    void shouldRedirectToProductListWhenProductIsMaterial() throws Exception {
        Product material = Product.builder()
                .id(1L)
                .active(true)
                .type(Product.ProductType.MATERIAL)
                .build();

        given(productRepository.findById(1L)).willReturn(Optional.of(material));

        mockMvc.perform(get("/produkty/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produkty"));
    }
    @Test
    @WithMockUser
    void listProducts_WithSorting_ReturnsSortedProducts() throws Exception {
        given(productRepository.findByActiveTrueAndType(eq(Product.ProductType.PRODUCT), any(Sort.class)))
                .willReturn(java.util.Collections.emptyList());

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/produkty").param("sort", "price_desc"))
                .andExpect(status().isOk())
                .andExpect(view().name("produkty"))
                .andExpect(model().attribute("currentSort", "price_desc"));
    }
}
