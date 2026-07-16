package org.example.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusHistoryTest {

    @Test
    void build_CreatesValidHistoryRecord() {
        OrderStatus status = OrderStatus.builder()
                .name("Zpracovává se")
                .colorClass("bg-yellow-100")
                .build();

        OrderStatusHistory history = OrderStatusHistory.builder()
                .status(status)
                .note("Zákazník požádal o urychlení dodání.")
                .build();

        assertEquals("Zpracovává se", history.getStatus().getName());
        assertEquals("Zákazník požádal o urychlení dodání.", history.getNote());
        assertNull(history.getCreatedAt(), "Před uložením do DB by měl být čas vytvoření null (řeší Hibernate).");
    }
}