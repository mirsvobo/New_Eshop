package org.example.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityConfigTest {

    @Autowired
    private MockMvc mockMvc;


    @Test
    @WithAnonymousUser
    void publicRoutes_ShouldBeAccessibleToAnonymous() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/login"))
                .andExpect(status().isOk());
    }


    @Test
    @WithAnonymousUser
    void customerRoutes_ShouldRedirectAnonymousToLogin() throws Exception {
        mockMvc.perform(get("/muj-ucet"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void customerRoutes_ShouldDenyAccessToEmployee() throws Exception {
        mockMvc.perform(get("/muj-ucet"))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithAnonymousUser
    void adminRoutes_ShouldRedirectAnonymousToLogin() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void adminRoutes_ShouldDenyAccessToCustomer() throws Exception {
        mockMvc.perform(get("/admin"))
                .andExpect(status().isForbidden());
    }


    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void strictAdminRoutes_ShouldDenyAccessToEmployee() throws Exception {
        mockMvc.perform(get("/admin/uzivatele"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void strictAdminRoutes_ShouldAllowAccessToAdmin() throws Exception {

        mockMvc.perform(get("/admin/uzivatele"))
                .andExpect(status().is(200));
    }


    @Test
    @WithMockUser(roles = "CUSTOMER")
    void attendanceRoutes_ShouldDenyAccessToCustomer() throws Exception {
        mockMvc.perform(get("/dochazka"))
                .andExpect(status().isForbidden());
    }
}