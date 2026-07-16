package org.example.service;

import org.example.model.User;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService userDetailsService;

    @Test
    void loadUserByUsername_UserExists_ReturnsUserDetails() {
        // Arrange
        User user = User.builder()
                .email("admin@erp.cz")
                .password("$2a$10$hashedPassword")
                .role(User.Role.ROLE_ADMIN)
                .active(true)
                .build();

        when(userRepository.findByEmail("admin@erp.cz")).thenReturn(Optional.of(user));

        // Act
        UserDetails userDetails = userDetailsService.loadUserByUsername("admin@erp.cz");

        // Assert
        assertNotNull(userDetails);
        assertEquals("admin@erp.cz", userDetails.getUsername());
        assertEquals("$2a$10$hashedPassword", userDetails.getPassword());
        assertTrue(userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN")), "Role by měla být namapována na ROLE_ADMIN");
    }

    @Test
    void loadUserByUsername_UserDisabled_ThrowsException() {
        // Arrange
        User user = User.builder()
                .email("blocked@erp.cz")
                .active(false)
                .build();

        when(userRepository.findByEmail("blocked@erp.cz")).thenReturn(Optional.of(user));

        // Act & Assert
        assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("blocked@erp.cz");
        });
    }

    @Test
    void loadUserByUsername_UserNotFound_ThrowsException() {
        // Arrange
        when(userRepository.findByEmail("neexistuje@erp.cz")).thenReturn(Optional.empty());

        // Act & Assert
        UsernameNotFoundException exception = assertThrows(UsernameNotFoundException.class, () -> {
            userDetailsService.loadUserByUsername("neexistuje@erp.cz");
        });

        assertTrue(exception.getMessage().contains("nebyl nalezen"));
    }
}