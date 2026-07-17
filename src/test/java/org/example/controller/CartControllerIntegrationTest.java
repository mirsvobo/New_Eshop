package org.example.controller;

import org.example.model.Product;
import org.example.model.TaxRate;
import org.example.repository.ProductRepository;
import org.example.repository.TaxRateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class CartControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TaxRateRepository taxRateRepository;

    private Product testProduct;

    @BeforeEach
    void setUp() {
        TaxRate tax = taxRateRepository.save(TaxRate.builder()
                .name("Test Tax")
                .rate(new BigDecimal("21"))
                .defaultRate(true)
                .build());

        testProduct = productRepository.save(Product.builder()
                .name("Integration Test Product")
                .price(new BigDecimal("100"))
                .taxRate(tax)
                .stockQuantity(10)
                .type(Product.ProductType.PRODUCT)
                .unit("ks")
                .active(true)
                .build());
    }

    @Test
    void addToCart_RedirectsToCartPage() throws Exception {
        mockMvc.perform(post("/kosik/pridat")
                        .with(csrf())
                        .param("productId", testProduct.getId().toString())
                        .param("quantity", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kosik"));
    }

    @Test
    void switchTaxMode_RedirectsToCartPage() throws Exception {
        mockMvc.perform(post("/kosik/rezim")
                        .with(csrf())
                        .param("taxMode", "REDUCED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/kosik"));
    }
}