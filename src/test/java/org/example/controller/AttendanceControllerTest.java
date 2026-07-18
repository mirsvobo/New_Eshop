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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;

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

    @Test
    @WithMockUser(roles = "ADMIN")
    void verifyPin_ValidPin_ReturnsWorkerInfo() throws Exception {
        User user = User.builder().firstName("Jan").lastName("Novak").build();
        given(attendanceService.authenticateByPin("1234")).willReturn(Optional.of(user));
        given(attendanceService.getCurrentStatus(user)).willReturn(org.example.service.AttendanceService.WorkerStatus.WORKING);

        mockMvc.perform(post("/dochazka/api/verify")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"1234\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.worker.name").value("Jan Novak"))
                .andExpect(jsonPath("$.worker.status").value("working"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void verifyPin_InvalidPin_ReturnsError() throws Exception {
        given(attendanceService.authenticateByPin("0000")).willReturn(Optional.empty());

        mockMvc.perform(post("/dochazka/api/verify")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"0000\"}")
                        .with(csrf()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void performAction_ValidPin_SavesAction() throws Exception {
        User user = User.builder().firstName("Jan").lastName("Novak").build();
        given(attendanceService.authenticateByPin("1234")).willReturn(Optional.of(user));

        mockMvc.perform(post("/dochazka/api/action")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"pin\":\"1234\", \"action\":\"clock_in\"}")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        org.mockito.Mockito.verify(attendanceService).recordAttendance(eq(user), eq(org.example.model.AttendanceRecord.AttendanceType.CLOCK_IN));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void listLogs_AccessibleByAdmin() throws Exception {
        User admin = User.builder().id(1L).email("admin@test.cz").role(User.Role.ROLE_ADMIN).build();
        given(userService.findByEmail("admin@test.cz")).willReturn(Optional.of(admin));
        given(attendanceService.getFilteredRecords(null, null, null)).willReturn(java.util.Collections.emptyList());

        // ZDE JE OPRAVA PRO THYMELEAF
        given(attendanceService.calculateSummary(any())).willReturn(java.util.Map.of("totalHours", "0 hod", "avgHours", "0 hod", "clockInCount", 0L, "entryCount", 0L));

        mockMvc.perform(get("/dochazka/admin"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/attendance-logs"))
                .andExpect(model().attributeExists("records"))
                .andExpect(model().attributeExists("summary"));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void manualEntryForm_AccessibleByAdmin() throws Exception {
        User admin = User.builder().id(1L).email("admin@test.cz").role(User.Role.ROLE_ADMIN).build();
        given(userService.findByEmail("admin@test.cz")).willReturn(Optional.of(admin));

        mockMvc.perform(get("/dochazka/admin/novy"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/attendance-form"))
                .andExpect(model().attributeExists("record"))
                .andExpect(model().attributeExists("users"));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void saveEntry_ByAdmin_ShouldRedirect() throws Exception {
        User admin = User.builder().id(1L).email("admin@test.cz").role(User.Role.ROLE_ADMIN).build();
        given(userService.findByEmail("admin@test.cz")).willReturn(Optional.of(admin));

        mockMvc.perform(post("/dochazka/admin/save")
                        .param("type", "CLOCK_IN")
                        .param("timestamp", "2024-04-15T08:00")
                        .param("employee.id", "2")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dochazka/admin"))
                .andExpect(flash().attributeExists("success"));

        org.mockito.Mockito.verify(attendanceService).save(any(org.example.model.AttendanceRecord.class));
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void editEntryForm_AccessibleByAdmin() throws Exception {
        User admin = User.builder().id(1L).email("admin@test.cz").role(User.Role.ROLE_ADMIN).build();
        given(userService.findByEmail("admin@test.cz")).willReturn(Optional.of(admin));
        given(attendanceService.findById(1L)).willReturn(Optional.of(new org.example.model.AttendanceRecord()));

        mockMvc.perform(get("/dochazka/admin/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/attendance-form"))
                .andExpect(model().attributeExists("record"));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void deleteEntry_ByAdmin_ShouldRedirect() throws Exception {
        User admin = User.builder().id(1L).email("admin@test.cz").role(User.Role.ROLE_ADMIN).build();
        given(userService.findByEmail("admin@test.cz")).willReturn(Optional.of(admin));

        org.example.model.AttendanceRecord record = new org.example.model.AttendanceRecord();
        record.setEmployee(admin);
        given(attendanceService.findById(1L)).willReturn(Optional.of(record));

        mockMvc.perform(post("/dochazka/admin/delete/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/dochazka/admin"))
                .andExpect(flash().attributeExists("success"));

        org.mockito.Mockito.verify(attendanceService).delete(1L);
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void showReport_WithUser_ShouldReturnView() throws Exception {
        User admin = User.builder().id(1L).email("admin@test.cz").role(User.Role.ROLE_ADMIN).build();
        given(userService.findByEmail("admin@test.cz")).willReturn(Optional.of(admin));
        given(userService.findById(2L)).willReturn(Optional.of(new User()));
        given(attendanceService.generateMonthlyReport(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .willReturn(java.util.Collections.emptyList());

        mockMvc.perform(get("/dochazka/admin/report").param("userId", "2"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/attendance-report"))
                .andExpect(model().attributeExists("report"));
    }

    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void exportReport_ShouldReturnExcel() throws Exception {
        User admin = User.builder().id(1L).email("admin@test.cz").role(User.Role.ROLE_ADMIN).build();
        User employee = User.builder().id(2L).lastName("Novak").build();
        given(userService.findByEmail("admin@test.cz")).willReturn(Optional.of(admin));
        given(userService.findById(2L)).willReturn(Optional.of(employee));
        given(attendanceService.exportMonthlyReportToExcel(any(), org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.anyInt()))
                .willReturn(new java.io.ByteArrayInputStream(new byte[0]));

        mockMvc.perform(get("/dochazka/admin/report/export")
                        .param("userId", "2")
                        .param("year", "2024")
                        .param("month", "4"))
                .andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().exists(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION));
    }
}