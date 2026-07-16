package org.example.controller;

import org.example.model.OrderStatus;
import org.example.service.Cart;
import org.example.service.OrderStatusService;
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
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminOrderStatusController.class)
class AdminOrderStatusControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderStatusService statusService;

    @MockitoBean(name = "cart")
    private Cart cart;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnListViewWhenListStatusesCalled() throws Exception {
        given(statusService.getAllOrdered()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/nastaveni/stavy"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/order-statuses"))
                .andExpect(model().attributeExists("statuses"))
                .andExpect(model().attributeExists("newStatus"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRedirectWhenSaveSuccessful() throws Exception {
        mockMvc.perform(post("/admin/nastaveni/stavy/ulozit")
                        .param("name", "Nová")
                        .param("colorClass", "bg-blue")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/nastaveni/stavy"))
                .andExpect(flash().attribute("success", "Stav objednávky byl úspěšně uložen."));

        verify(statusService).save(any(OrderStatus.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldToggleStatusSuccessfully() throws Exception {
        mockMvc.perform(post("/admin/nastaveni/stavy/toggle-status/1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/nastaveni/stavy"))
                .andExpect(flash().attribute("success", "Dostupnost stavu byla změněna."));

        verify(statusService).toggleActive(eq(1L));
    }
}