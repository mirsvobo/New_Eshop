package org.example.controller;

import org.example.model.User;
import org.example.repository.ProductRepository;
import org.example.repository.UserRepository;
import org.example.service.InventoryService;
import org.example.service.ProductService;
import org.example.service.Cart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InventoryController.class)
class InventoryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InventoryService inventoryService;
    @MockitoBean
    private ProductService productService;
    @MockitoBean
    private ProductRepository productRepository;
    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean(name = "cart")
    private Cart cart;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnSkladViewWithPreFilteredProducts() throws Exception {
        given(inventoryService.getFilteredMovements(any(), any(), any(), any(), any(), any()))
                .willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/sklad"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/sklad"))
                .andExpect(model().attributeExists("materials"))
                .andExpect(model().attributeExists("finalProducts"));
    }

    // --- NOVÉ TESTY DLE POŽADAVKŮ (KROK 2 - VÝROBA) ---

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void produceProduct_ShouldCallServiceAndRedirectWithSuccessMessage() throws Exception {
        User mockUser = User.builder().email("admin@test.cz").build();
        given(userRepository.findByEmail("admin@test.cz")).willReturn(Optional.of(mockUser));

        mockMvc.perform(post("/admin/sklad/vyroba")
                        .param("productId", "1")
                        .param("quantity", "15")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/sklad"))
                .andExpect(flash().attributeExists("success"));

        verify(inventoryService).produceProduct(eq(1L), eq(15.0), any(User.class));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void produceProduct_ShouldRedirectWithErrorMessage_WhenExceptionIsThrown() throws Exception {
        User mockUser = User.builder().email("admin@test.cz").build();
        given(userRepository.findByEmail("admin@test.cz")).willReturn(Optional.of(mockUser));

        doThrow(new IllegalArgumentException("Do výroby lze zadat pouze produkty typu PRODUCT."))
                .when(inventoryService).produceProduct(eq(2L), eq(10.0), any(User.class));

        mockMvc.perform(post("/admin/sklad/vyroba")
                        .param("productId", "2")
                        .param("quantity", "10")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/sklad"))
                .andExpect(flash().attributeExists("error"));
    }
}