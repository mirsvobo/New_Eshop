package org.example.controller;

import org.example.model.Product;
import org.example.repository.ProductRepository;
import org.example.service.Cart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;

    @MockitoBean(name = "cart")
    private Cart cart;

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
        Product product = Product.builder()
                .id(1L)
                .active(true)
                .type(Product.ProductType.PRODUCT)
                .build();
        given(productRepository.findById(1L)).willReturn(Optional.of(product));

        mockMvc.perform(get("/produkty/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("produkt-detail"))
                .andExpect(model().attribute("product", product));
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
}