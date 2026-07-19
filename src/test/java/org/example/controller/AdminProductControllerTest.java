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
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
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

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void showAddProductForm_ShouldReturnForm() throws Exception {
        given(taxRateRepository.findAll()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/produkty/novy"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("taxes"));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void showEditProductForm_ShouldReturnForm() throws Exception {
        Product product = new Product();
        product.setId(1L);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(taxRateRepository.findAll()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/produkty/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(model().attribute("product", product))
                .andExpect(model().attributeExists("taxes"));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void saveProduct_Success_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/admin/produkty/save")
                        .param("name", "Valid Product")
                        .param("price", "100.00")
                        .param("stockQuantity", "10")
                        .param("minStockLevel", "2")
                        .param("unit", "ks")
                        .param("type", "PRODUCT")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty"))
                .andExpect(flash().attributeExists("success"));

        verify(productService).saveProduct(any(Product.class), eq(null), any(User.class));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void saveProduct_WithDimensions_Success_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/admin/produkty/save")
                        .param("name", "Valid Product with Dimensions")
                        .param("price", "100.00")
                        .param("stockQuantity", "10")
                        .param("minStockLevel", "2")
                        .param("unit", "ks")
                        .param("type", "PRODUCT")
                        .param("width", "120.5")
                        .param("depth", "80.0")
                        .param("height", "200.0")
                        .param("volume", "1.92")
                        .param("additionalDimensions", "Přesah střechy: 10 cm")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty"))
                .andExpect(flash().attributeExists("success"));

        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productService).saveProduct(productCaptor.capture(), eq(null), any(User.class));

        Product capturedProduct = productCaptor.getValue();
        assertEquals(120.5, capturedProduct.getWidth());
        assertEquals(80.0, capturedProduct.getDepth());
        assertEquals(200.0, capturedProduct.getHeight());
        assertEquals(1.92, capturedProduct.getVolume());
        assertEquals("Přesah střechy: 10 cm", capturedProduct.getAdditionalDimensions());
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void manageRecipe_ShouldReturnRecipeView() throws Exception {
        Product product = new Product();
        product.setId(1L);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(productService.getAllMaterials()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/produkty/1/kusovnik"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-recipe"))
                .andExpect(model().attribute("product", product))
                .andExpect(model().attributeExists("materials"));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void addRecipeItem_Success_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/admin/produkty/1/kusovnik/pridat")
                        .param("materialId", "2")
                        .param("quantity", "5")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty/1/kusovnik"))
                .andExpect(flash().attributeExists("success"));

        verify(productService).addRecipeItem(eq(1L), eq(2L), eq(5), any(User.class));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void deleteRecipeItem_Success_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/admin/produkty/1/kusovnik/smazat/2")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty/1/kusovnik"))
                .andExpect(flash().attributeExists("success"));

        verify(productService).deleteRecipeItem(eq(1L), eq(2L), any(User.class));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void deleteProduct_Success_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/admin/produkty/delete/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty"))
                .andExpect(flash().attributeExists("success"));

        verify(productService).deleteProduct(eq(1L), any());
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void listProducts_ShouldReturnView() throws Exception {
        given(productRepository.findFilteredProducts(any(), any(), any(), any())).willReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/admin/produkty"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/produkty"))
                .andExpect(model().attributeExists("products"));
    }
}