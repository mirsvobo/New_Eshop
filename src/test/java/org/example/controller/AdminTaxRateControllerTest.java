package org.example.controller;

import org.example.model.TaxRate;
import org.example.service.Cart;
import org.example.service.TaxRateService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminTaxRateController.class)
class AdminTaxRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaxRateService taxRateService;

    @MockitoBean(name = "cart")
    private Cart cart;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnListViewWithTaxRates() throws Exception {
        given(taxRateService.findAll()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/tax-rates"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tax-rates"))
                .andExpect(model().attributeExists("taxRates"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRedirectWhenSaveTaxRateSuccessful() throws Exception {
        mockMvc.perform(post("/admin/tax-rates/save")
                        .param("name", "DPH 21%")
                        .param("rate", "21.0")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tax-rates"))
                .andExpect(flash().attribute("success", "Daňová sazba byla úspěšně uložena."));

        verify(taxRateService).save(any(TaxRate.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRedirectWithSuccessAfterDeletion() throws Exception {
        mockMvc.perform(get("/admin/tax-rates/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/tax-rates"))
                .andExpect(flash().attribute("success", "Daňová sazba byla odstraněna."));

        verify(taxRateService).delete(eq(1L));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRedirectWithErrorWhenDeletionFails() throws Exception {
        doThrow(new RuntimeException("In use")).when(taxRateService).delete(1L);

        mockMvc.perform(get("/admin/tax-rates/delete/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("error"));
    }
    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldShowCreateForm() throws Exception {
        mockMvc.perform(get("/admin/tax-rates/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tax-form"))
                .andExpect(model().attributeExists("taxRate"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldShowEditForm() throws Exception {
        TaxRate taxRate = new TaxRate();
        taxRate.setId(1L);
        given(taxRateService.findById(1L)).willReturn(taxRate);

        mockMvc.perform(get("/admin/tax-rates/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/tax-form"))
                .andExpect(model().attribute("taxRate", taxRate));
    }
}