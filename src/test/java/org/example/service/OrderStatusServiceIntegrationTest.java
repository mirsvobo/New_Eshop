package org.example.service;

import org.example.model.OrderStatus;
import org.example.repository.OrderStatusRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class OrderStatusServiceIntegrationTest {

    @Autowired
    private OrderStatusService orderStatusService;

    @Autowired
    private OrderStatusRepository orderStatusRepository;

    @Test
    void testOrderStatusLifecycle() {
        // 1. Uložení nového stavu
        OrderStatus status = new OrderStatus();
        status.setName("Testovací Stav");
        status.setColorClass("bg-test-100");
        status.setDisplayOrder(99);
        status.setActive(true);

        orderStatusService.save(status);
        assertNotNull(status.getId(), "Stav musí mít po uložení vygenerované ID");

        // 2. Nalezení podle ID
        OrderStatus foundStatus = orderStatusService.findById(status.getId());
        assertEquals("Testovací Stav", foundStatus.getName());

        // 3. Získání všech seřazených stavů
        List<OrderStatus> allStatuses = orderStatusService.getAllOrdered();
        assertFalse(allStatuses.isEmpty(), "Seznam stavů nesmí být prázdný");
        assertTrue(allStatuses.stream().anyMatch(s -> s.getName().equals("Testovací Stav")));

        // 4. Přepnutí aktivity (Toggle)
        orderStatusService.toggleActive(status.getId());
        OrderStatus toggledStatus = orderStatusService.findById(status.getId());
        assertFalse(toggledStatus.isActive(), "Stav by měl být po přepnutí neaktivní");

        // 5. Test vyhození výjimky pro neexistující ID
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            orderStatusService.findById(99999L);
        });
        assertTrue(exception.getMessage().contains("nenalezen"));
    }
}