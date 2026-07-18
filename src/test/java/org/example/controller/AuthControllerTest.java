package org.example.controller;

import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.Cart;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@Import(org.example.config.SecurityConfig.class)
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UserRepository userRepository;

    @MockitoBean
    private PasswordEncoder passwordEncoder;

    @MockitoBean(name = "cart")
    private Cart cart;

    @Test
    void loginPage_LoadsSuccessfully_AndReturnsCorrectView() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"));
    }

    @Test
    void registerPage_LoadsSuccessfully_AndReturnsCorrectView() throws Exception {
        mockMvc.perform(get("/registrace"))
                .andExpect(status().isOk())
                .andExpect(view().name("register"));
    }

    @Test
    void myAccountPage_Unauthenticated_RedirectsToLogin() throws Exception {
        mockMvc.perform(get("/muj-ucet"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void processRegistration_Success_RedirectsToLogin() throws Exception {
        when(userRepository.findByEmail("newuser@test.cz")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("encoded");

        mockMvc.perform(post("/registrace")
                        .param("email", "newuser@test.cz")
                        .param("password", "password")
                        .param("firstName", "Jan")
                        .param("lastName", "Novak")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"))
                .andExpect(flash().attributeExists("success"));

        verify(userRepository).save(any(User.class));
    }

    @Test
    void processRegistration_ExistingEmail_RedirectsToRegister() throws Exception {
        User existingUser = new User();
        when(userRepository.findByEmail("existing@test.cz")).thenReturn(Optional.of(existingUser));

        mockMvc.perform(post("/registrace")
                        .param("email", "existing@test.cz")
                        .param("password", "password")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/registrace"))
                .andExpect(flash().attributeExists("error"));

        verify(userRepository, never()).save(any(User.class));
    }
}