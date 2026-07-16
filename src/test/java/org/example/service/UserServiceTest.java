package org.example.service;

import org.example.model.User;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuditService auditService;
    @InjectMocks
    private UserService userService;

    private User testEmployee;
    private User adminUser;

    @BeforeEach
    void setUp() {
        testEmployee = User.builder()
                .id(1L)
                .email("test@erp.cz")
                .password("oldEncryptedHash")
                .role(User.Role.ROLE_EMPLOYEE)
                .active(true)
                .build();

        adminUser = User.builder()
                .id(2L)
                .email("admin@erp.cz")
                .password("adminHash")
                .role(User.Role.ROLE_ADMIN)
                .active(true)
                .build();
    }

    @Test
    void saveUser_NewUser_EncodesPassword() {
        User newUser = User.builder()
                .email("new@erp.cz")
                .role(User.Role.ROLE_CUSTOMER)
                .active(true)
                .build();

        when(passwordEncoder.encode("secret123")).thenReturn("newEncodedHash");

        userService.saveUser(newUser, "secret123");

        verify(passwordEncoder, times(1)).encode("secret123");
        assertEquals("newEncodedHash", newUser.getPassword());
        verify(userRepository, times(1)).save(newUser);
    }

    @Test
    void saveUser_ExistingUser_NewPassword_EncodesPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));
        when(passwordEncoder.encode("newSecret")).thenReturn("newHash");

        userService.saveUser(testEmployee, "newSecret");

        verify(passwordEncoder, times(1)).encode("newSecret");
        assertEquals("newHash", testEmployee.getPassword());
        verify(userRepository, times(1)).save(testEmployee);
    }

    @Test
    void saveUser_ExistingUser_NoNewPassword_KeepsOldPassword() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testEmployee));

        userService.saveUser(testEmployee, "");

        verify(passwordEncoder, never()).encode(anyString());
        assertEquals("oldEncryptedHash", testEmployee.getPassword());
        verify(userRepository, times(1)).save(testEmployee);
    }


    @Test
    void saveUser_ChangeRoleOfLastAdmin_ThrowsException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRoleAndActiveTrue(User.Role.ROLE_ADMIN)).thenReturn(1L);

        User modifiedAdmin = User.builder().id(2L).role(User.Role.ROLE_EMPLOYEE).active(true).build();

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            userService.saveUser(modifiedAdmin, null);
        });

        assertTrue(exception.getMessage().contains("administrátor"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void toggleUserStatus_LastAdmin_ThrowsException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRoleAndActiveTrue(User.Role.ROLE_ADMIN)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> {
            userService.toggleUserStatus(2L);
        });

        verify(userRepository, never()).save(any());
    }

    @Test
    void deleteUser_LastAdmin_ThrowsException() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRoleAndActiveTrue(User.Role.ROLE_ADMIN)).thenReturn(1L);

        assertThrows(IllegalStateException.class, () -> {
            userService.deleteUser(2L);
        });

        verify(userRepository, never()).deleteById(anyLong());
    }

    @Test
    void deleteUser_Success_IfMoreAdminsExist() {
        when(userRepository.findById(2L)).thenReturn(Optional.of(adminUser));
        when(userRepository.countByRoleAndActiveTrue(User.Role.ROLE_ADMIN)).thenReturn(2L);

        userService.deleteUser(2L);

        verify(userRepository, times(1)).deleteById(2L);
    }
}