package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditService auditService;

    private static final String MODULE_NAME = "UŽIVATELÉ";

    public long count() {
        return userRepository.count();
    }

    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    public List<User> getUsersByRole(User.Role role) {
        if (role == null) {
            return getAllUsers();
        }
        return userRepository.findByRole(role);
    }

    public User getUserById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Uživatel nenalezen: " + id));
    }

    public List<User> findAll() {
        return userRepository.findAll();
    }

    public Optional<User> findById(Long id) {
        return userRepository.findById(id);
    }

    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    public List<User> getActiveEmployees() {
        return userRepository.findByRoleAndActiveTrue(User.Role.ROLE_EMPLOYEE);
    }

    @Transactional
    public void promoteToAdmin(Long userId) {
        User user = getUserById(userId);
        user.setRole(User.Role.ROLE_ADMIN);
        user.setActive(true);
        userRepository.save(user);

        auditService.log(MODULE_NAME, "Povýšení", "Uživatel s ID " + userId + " (" + user.getEmail() + ") byl povýšen na Administrátora.");
    }

    @Transactional
    public void saveUser(User userForm, String rawPassword) {
        User userToSave;
        boolean isNewUser = (userForm.getId() == null);

        if (!isNewUser) {
            userToSave = getUserById(userForm.getId());
            if (userToSave.getRole() == User.Role.ROLE_ADMIN && userToSave.isActive()) {
                if (userForm.getRole() != User.Role.ROLE_ADMIN || !userForm.isActive()) {
                    long activeAdminCount = userRepository.countByRoleAndActiveTrue(User.Role.ROLE_ADMIN);
                    if (activeAdminCount <= 1) {
                        throw new IllegalStateException("Pokus o změnu posledního administrátora.");
                    }
                }
            }
            userToSave.setFirstName(userForm.getFirstName());
            userToSave.setLastName(userForm.getLastName());
            userToSave.setEmail(userForm.getEmail());
            userToSave.setPhone(userForm.getPhone());
            userToSave.setRole(userForm.getRole());
            userToSave.setActive(userForm.isActive());
            userToSave.setPin(userForm.getPin() != null && !userForm.getPin().isBlank() ? userForm.getPin() : null);
            userToSave.setCompanyName(userForm.getCompanyName());
            userToSave.setIco(userForm.getIco());
            userToSave.setDic(userForm.getDic());
            userToSave.setBillingAddress(userForm.getBillingAddress());
            userToSave.setDeliveryAddress(userForm.getDeliveryAddress());

            if (rawPassword != null && !rawPassword.isBlank()) {
                userToSave.setPassword(passwordEncoder.encode(rawPassword));
                auditService.log(MODULE_NAME, "Změna hesla", "Změněno heslo pro uživatele: " + userToSave.getEmail());
            }
        } else {
            userToSave = userForm;
            if (rawPassword != null && !rawPassword.isBlank()) {
                userToSave.setPassword(passwordEncoder.encode(rawPassword));
            }
            if (userToSave.getPin() != null && userToSave.getPin().isBlank()) {
                userToSave.setPin(null);
            }
        }

        userRepository.save(userToSave);

        if (isNewUser) {
            auditService.log(MODULE_NAME, "Nový uživatel", "Byl vytvořen nový uživatel: " + userToSave.getEmail() + " s rolí " + userToSave.getRole());
        } else {
            auditService.log(MODULE_NAME, "Úprava profilu", "Byly upraveny údaje uživatele s ID " + userToSave.getId() + " (" + userToSave.getEmail() + ").");
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        User user = getUserById(id);
        if (user.getRole() == User.Role.ROLE_ADMIN && user.isActive()) {
            long activeAdminCount = userRepository.countByRoleAndActiveTrue(User.Role.ROLE_ADMIN);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Pokus o smazání posledního administrátora.");
            }
        }

        String userEmail = user.getEmail();
        userRepository.deleteById(id);

        auditService.log(MODULE_NAME, "Smazání", "Byl trvale smazán uživatel s ID " + id + " (" + userEmail + ").");
    }

    @Transactional
    public void toggleUserStatus(Long id) {
        User user = getUserById(id);
        if (user.isActive() && user.getRole() == User.Role.ROLE_ADMIN) {
            long activeAdminCount = userRepository.countByRoleAndActiveTrue(User.Role.ROLE_ADMIN);
            if (activeAdminCount <= 1) {
                throw new IllegalStateException("Pokus o deaktivaci posledního administrátora.");
            }
        }

        boolean newStatus = !user.isActive();
        user.setActive(newStatus);
        userRepository.save(user);

        String actionState = newStatus ? "Aktivován" : "Deaktivován";
        auditService.log(MODULE_NAME, "Změna stavu", "Uživatel s ID " + id + " (" + user.getEmail() + ") byl " + actionState.toLowerCase() + ".");
    }
}