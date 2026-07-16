package org.example.controller;

import org.example.model.Product;
import org.example.model.User;
import org.example.repository.ProductRepository;
import org.example.repository.TaxRateRepository;
import org.example.repository.UserRepository;
import org.example.service.Cart;
import org.example.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminProductController.class)
class AdminProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductRepository productRepository;
    @MockitoBean
    private ProductService productService;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private TaxRateRepository taxRateRepository;

    @MockitoBean(name = "cart")
    private Cart cart;

    private User adminUser;

    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .email("admin@test.cz")
                .firstName("Admin")
                .build();
        given(userRepository.findByEmail("admin@test.cz")).willReturn(Optional.of(adminUser));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void shouldReturnListViewWithFilteredProductsWhenListProductsCalled() throws Exception {
        given(productRepository.findFilteredProducts(anyString(), any(), any(), any(Sort.class)))
                .willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/produkty")
                        .param("search", "test")
                        .param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/produkty"));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void shouldReturnFormWithErrorsWhenValidationFails() throws Exception {

        mockMvc.perform(post("/admin/produkty/save")
                        .param("name", "Testovací produkt")
                        .param("price", "chybna_hodnota")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(model().hasErrors());
    }
}