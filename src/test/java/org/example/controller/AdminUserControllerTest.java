package org.example.controller;

import org.example.model.User;
import org.example.repository.OrderRepository;
import org.example.service.Cart;
import org.example.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminUserController.class)
class AdminUserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private OrderRepository orderRepository;

    @MockitoBean(name = "cart")
    private Cart cart;

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldReturnUserListWhenNoFilterProvided() throws Exception {
        given(userService.getAllUsers()).willReturn(Collections.emptyList());

        mockMvc.perform(get("/admin/uzivatele"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-list"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldShowEditFormWithCalculatedTotalSpent() throws Exception {
        User user = User.builder().id(1L).email("user@test.cz").build();
        given(userService.getUserById(1L)).willReturn(user);
        given(orderRepository.sumTotalAmountByCustomer(user)).willReturn(new BigDecimal("1500.00"));

        mockMvc.perform(get("/admin/uzivatele/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/user-form"))
                .andExpect(model().attribute("totalSpent", new BigDecimal("1500.00")));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldRedirectToTransferRightsWhenSavingLastAdminFails() throws Exception {
        doThrow(new IllegalStateException("Pokus o změnu posledního administrátora."))
                .when(userService).saveUser(any(User.class), any());

        mockMvc.perform(post("/admin/uzivatele/ulozit")
                        .param("email", "admin@test.cz")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/uzivatele/prevod-prav"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldToggleStatusSuccessfully() throws Exception {
        mockMvc.perform(post("/admin/uzivatele/zmenit-stav/1")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/uzivatele"))
                .andExpect(flash().attributeExists("success"));

        verify(userService).toggleUserStatus(1L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void shouldTransferAdminRightsSuccessfully() throws Exception {
        mockMvc.perform(post("/admin/uzivatele/prevod-prav")
                        .param("newAdminId", "2")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/uzivatele"))
                .andExpect(flash().attribute("success", "Administrátorská práva byla úspěšně předána."));

        verify(userService).promoteToAdmin(2L);
    }
}