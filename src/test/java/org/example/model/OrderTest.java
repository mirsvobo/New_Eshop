package org.example.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class OrderTest {

    @Test
    void newOrder_HasEmptyCollectionsInitialized() {
        Order order = new Order();

        assertNotNull(order.getItems(), "Kolekce položek musí být inicializována jako prázdný list.");
        assertTrue(order.getItems().isEmpty());

        assertNotNull(order.getStatusHistory(), "Kolekce historie stavů musí být inicializována jako prázdný list.");
        assertTrue(order.getStatusHistory().isEmpty());
    }

    @Test
    void getCustomerFullName_ReturnsCorrectFormat() {
        User user = User.builder().firstName("Karel").lastName("Novák").build();
        Order order = Order.builder().customer(user).build();

        assertEquals("Karel Novák", order.getCustomerFullName());
    }

    @Test
    void getCustomerFullName_ReturnsGuestFormat() {
        Order order = Order.builder()
                .guestFirstName("Anonym")
                .guestLastName("Zákazník")
                .build();


        assertEquals("Anonym Zákazník", order.getCustomerFullName());
    }
}