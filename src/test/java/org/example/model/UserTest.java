package org.example.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void getFullName_ReturnsCorrectlyFormattedName() {
        User user = User.builder()
                .firstName("Karel")
                .lastName("Omáčka")
                .build();

        assertEquals("Karel Omáčka", user.getFullName(), "Metoda getFullName musí spojit jméno a příjmení mezerou.");
    }

    @Test
    void defaultActive_IsTrue() {
        User user = new User();

        assertTrue(user.isActive(), "Nový uživatel by měl mít z bezpečnostních důvodů povolený účet jako výchozí hodnotu (pokud není nastaveno jinak).");
    }

    @Test
    void roleEnum_ContainsRequiredRoles() {
        assertNotNull(User.Role.valueOf("ROLE_ADMIN"));
        assertNotNull(User.Role.valueOf("ROLE_EMPLOYEE"));
        assertNotNull(User.Role.valueOf("ROLE_CUSTOMER"));
    }
}