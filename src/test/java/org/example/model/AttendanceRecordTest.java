package org.example.model;

import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AttendanceRecordTest {

    @Test
    void builder_CreatesValidRecord() {
        LocalDateTime now = LocalDateTime.now();
        User employee = new User();
        employee.setId(5L);

        AttendanceRecord record = AttendanceRecord.builder()
                .employee(employee)
                .type(AttendanceRecord.AttendanceType.CLOCK_IN)
                .timestamp(now)
                .build();

        assertEquals(AttendanceRecord.AttendanceType.CLOCK_IN, record.getType());
        assertEquals(now, record.getTimestamp());
        assertNotNull(record.getEmployee());
        assertEquals(5L, record.getEmployee().getId());
    }
}