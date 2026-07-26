package org.example.controller;

import org.example.dto.CartItemDto;
import org.example.model.InstallationPost;
import org.example.model.TaxMode;
import org.example.repository.InstallationPostRepository;
import org.example.service.Cart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class HomeControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "cart")
    private Cart cart;

    @MockitoBean
    private InstallationPostRepository installationPostRepository;

    @BeforeEach
    void setUp() {
        when(cart.getTaxMode()).thenReturn(TaxMode.STANDARD);
        when(cart.getTotalItems()).thenReturn(0);
    }

    @Test
    void homePage_LoadsSuccessfully() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));
    }

    @Test
    void homePage_AddsActiveInstallationPostsOrderedByAssemblyDateDescending() throws Exception {
        InstallationPost newestPost = InstallationPost.builder()
                .id(2L)
                .title("Montáž dřevníku XXL")
                .productName("Dřevník XXL")
                .assemblyDate(LocalDate.of(2026, 7, 18))
                .content("Dokončená montáž dřevníku XXL u rodinného domu.")
                .active(true)
                .build();

        InstallationPost olderPost = InstallationPost.builder()
                .id(1L)
                .title("Montáž dřevníku Klasik")
                .productName("Dřevník Klasik")
                .assemblyDate(LocalDate.of(2026, 7, 11))
                .content("Montáž dřevníku Klasik na připravený betonový základ.")
                .active(true)
                .build();

        List<InstallationPost> expectedPosts = List.of(newestPost, olderPost);

        when(installationPostRepository.findAllByActiveTrueOrderByAssemblyDateDesc())
                .thenReturn(expectedPosts);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attributeExists("installationPosts"))
                .andExpect(model().attribute("installationPosts", expectedPosts));

        verify(installationPostRepository)
                .findAllByActiveTrueOrderByAssemblyDateDesc();
    }

    @Test
    void checkoutPage_LoadsSuccessfully_WithItemsInCart() throws Exception {
        CartItemDto item = CartItemDto.builder()
                .productId(1L)
                .productName("Testovací produkt")
                .quantity(1)
                .price(new BigDecimal("100.00"))
                .basePrice(new BigDecimal("82.64"))
                .taxRateValue(new BigDecimal("21.0"))
                .build();

        when(cart.getItems()).thenReturn(List.of(item));
        when(cart.getTotalPrice()).thenReturn(new BigDecimal("100.00"));
        when(cart.getTaxMode()).thenReturn(TaxMode.STANDARD);

        mockMvc.perform(get("/kosik/pokladna"))
                .andExpect(status().isOk())
                .andExpect(view().name("checkout"));
    }

    @Test
    void aboutUsPage_LoadsSuccessfully() throws Exception {
        mockMvc.perform(get("/o-nas"))
                .andExpect(status().isOk())
                .andExpect(view().name("o-nas"));
    }

    @Test
    void contactPage_LoadsSuccessfully() throws Exception {
        mockMvc.perform(get("/kontakt"))
                .andExpect(status().isOk())
                .andExpect(view().name("kontakt"));
    }
}