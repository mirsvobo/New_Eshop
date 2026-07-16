package org.example.controller;

import org.example.model.User;
import org.example.service.AttendanceService;
import org.example.service.AuditService;
import org.example.service.Cart;
import org.example.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AttendanceController.class)
class AttendanceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AttendanceService attendanceService;

    @MockitoBean
    private UserService userService;

    @MockitoBean
    private AuditService auditService;

    @MockitoBean(name = "cart")
    private Cart cart;

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void attendanceReport_AccessibleByAdmin() throws Exception {
        User admin = User.builder()
                .id(1L)
                .email("admin@test.cz")
                .role(User.Role.ROLE_ADMIN)
                .build();

        given(userService.findByEmail("admin@test.cz")).willReturn(Optional.of(admin));

        mockMvc.perform(get("/dochazka/admin/report"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/attendance-report"));
    }
}