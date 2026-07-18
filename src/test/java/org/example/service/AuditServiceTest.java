package org.example.service;

import org.example.model.AuditLog;
import org.example.model.User;
import org.example.repository.AuditLogRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

class AuditServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SecurityContext securityContext;

    @Mock
    private Authentication authentication;

    @InjectMocks
    private AuditService auditService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void log_WithAuthenticatedUser_ShouldSaveLogWithUsername() {
        String module = "Product";
        String action = "CREATE";
        String details = "Created product: Test";
        String username = "admin@test.cz";

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(authentication.getPrincipal()).thenReturn(username);
        when(authentication.getName()).thenReturn(username);

        User mockUser = new User();
        mockUser.setEmail(username);
        when(userRepository.findByEmail(username)).thenReturn(Optional.of(mockUser));

        auditService.log(module, action, details);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        verify(auditLogRepository, timeout(2000)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(module, savedLog.getModule());
        assertEquals(action, savedLog.getAction());
        assertEquals(details, savedLog.getDetails());
        assertEquals(username, savedLog.getUser().getEmail());
    }

    @Test
    void log_WithoutAuthenticatedUser_ShouldSaveLogWithSystemUsername() {
        String module = "Order";
        String action = "UPDATE";
        String details = "Updated order status";

        SecurityContextHolder.setContext(securityContext);
        when(securityContext.getAuthentication()).thenReturn(null);

        auditService.log(module, action, details);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);

        verify(auditLogRepository, timeout(2000)).save(captor.capture());

        AuditLog savedLog = captor.getValue();
        assertEquals(module, savedLog.getModule());
        assertEquals(action, savedLog.getAction());
        assertEquals(details, savedLog.getDetails());
        assertNull(savedLog.getUser());
    }
    @Test
    void getFilteredLogs_WithUserAndModule_ReturnsFiltered() {
        Long userId = 1L;
        String module = "PRODUKTY";

        auditService.getFilteredLogs(userId, module);

        verify(auditLogRepository).findByUserIdAndModuleOrderByTimestampDesc(userId, module);
    }

    @Test
    void getFilteredLogs_WithOnlyUser_ReturnsFiltered() {
        Long userId = 1L;

        auditService.getFilteredLogs(userId, null);

        verify(auditLogRepository).findByUserIdOrderByTimestampDesc(userId);
    }

    @Test
    void getFilteredLogs_WithOnlyModule_ReturnsFiltered() {
        String module = "PRODUKTY";

        auditService.getFilteredLogs(null, module);

        verify(auditLogRepository).findByModuleOrderByTimestampDesc(module);
    }

    @Test
    void getFilteredLogs_WithoutFilters_ReturnsAllLogs() {
        auditService.getFilteredLogs(null, null);

        verify(auditLogRepository).findAllByOrderByTimestampDesc();
    }

    @Test
    void getAllModules_ReturnsExpectedList() {
        java.util.List<String> modules = auditService.getAllModules();

        org.junit.jupiter.api.Assertions.assertEquals(6, modules.size());
        org.junit.jupiter.api.Assertions.assertTrue(modules.contains("DOCHÁZKA"));
        org.junit.jupiter.api.Assertions.assertTrue(modules.contains("SYSTÉM"));
    }
}