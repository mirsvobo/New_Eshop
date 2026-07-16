package org.example.service;

import org.example.dto.AttendanceReportDto;
import org.example.model.AttendanceRecord;
import org.example.model.User;
import org.example.repository.AttendanceRecordRepository;
import org.example.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AttendanceRecordRepository attendanceRepository;

    @InjectMocks
    private AttendanceService attendanceService;

    private User testEmployee;
    private final int testYear = 2024;
    private final int testMonth = 4;

    @BeforeEach
    void setUp() {
        testEmployee = User.builder()
                .id(1L)
                .email("employee@test.com")
                .role(User.Role.ROLE_EMPLOYEE)
                .build();
    }

    @Test
    void testGenerateMonthlyReport_ShiftUnder6Hours_NoBreak() {
        List<AttendanceRecord> records = Arrays.asList(
                createRecord(8, 0, AttendanceRecord.AttendanceType.CLOCK_IN),
                createRecord(12, 0, AttendanceRecord.AttendanceType.CLOCK_OUT)
        );

        when(attendanceRepository.findByEmployeeOrderByTimestampDesc(testEmployee)).thenReturn(records);

        List<AttendanceReportDto> report = attendanceService.generateMonthlyReport(testEmployee, testYear, testMonth);

        assertEquals(1, report.size());
        AttendanceReportDto dayReport = report.get(0);

        assertEquals(Duration.ZERO, dayReport.getTotalBreakTime());
        assertEquals(Duration.ofHours(4), dayReport.getNetWorkTime());
        assertFalse(dayReport.isMissingClockOut());
        assertFalse(dayReport.isCappedAtMax());
    }

    @Test
    void testGenerateMonthlyReport_ShiftOver6Hours_NoBreak() {
        List<AttendanceRecord> records = Arrays.asList(
                createRecord(8, 0, AttendanceRecord.AttendanceType.CLOCK_IN),
                createRecord(16, 0, AttendanceRecord.AttendanceType.CLOCK_OUT)
        );

        when(attendanceRepository.findByEmployeeOrderByTimestampDesc(testEmployee)).thenReturn(records);

        List<AttendanceReportDto> report = attendanceService.generateMonthlyReport(testEmployee, testYear, testMonth);

        assertEquals(1, report.size());
        AttendanceReportDto dayReport = report.get(0);

        assertEquals(Duration.ofMinutes(30), dayReport.getTotalBreakTime());
        assertEquals(Duration.ofHours(7).plusMinutes(30), dayReport.getNetWorkTime());
    }

    @Test
    void testGenerateMonthlyReport_ShiftOver6Hours_ShortBreak() {
        List<AttendanceRecord> records = Arrays.asList(
                createRecord(8, 0, AttendanceRecord.AttendanceType.CLOCK_IN),
                createRecord(12, 0, AttendanceRecord.AttendanceType.BREAK_START),
                createRecord(12, 15, AttendanceRecord.AttendanceType.BREAK_END),
                createRecord(16, 0, AttendanceRecord.AttendanceType.CLOCK_OUT)
        );

        when(attendanceRepository.findByEmployeeOrderByTimestampDesc(testEmployee)).thenReturn(records);

        List<AttendanceReportDto> report = attendanceService.generateMonthlyReport(testEmployee, testYear, testMonth);

        assertEquals(1, report.size());
        AttendanceReportDto dayReport = report.get(0);

        assertEquals(Duration.ofMinutes(30), dayReport.getTotalBreakTime());
        assertEquals(Duration.ofHours(7).plusMinutes(30), dayReport.getNetWorkTime());
    }

    @Test
    void testGenerateMonthlyReport_ShiftOver6Hours_LongBreak() {
        List<AttendanceRecord> records = Arrays.asList(
                createRecord(8, 0, AttendanceRecord.AttendanceType.CLOCK_IN),
                createRecord(12, 0, AttendanceRecord.AttendanceType.BREAK_START),
                createRecord(12, 45, AttendanceRecord.AttendanceType.BREAK_END),
                createRecord(16, 0, AttendanceRecord.AttendanceType.CLOCK_OUT)
        );

        when(attendanceRepository.findByEmployeeOrderByTimestampDesc(testEmployee)).thenReturn(records);

        List<AttendanceReportDto> report = attendanceService.generateMonthlyReport(testEmployee, testYear, testMonth);

        assertEquals(1, report.size());
        AttendanceReportDto dayReport = report.get(0);

        assertEquals(Duration.ofMinutes(45), dayReport.getTotalBreakTime());
        assertEquals(Duration.ofHours(7).plusMinutes(15), dayReport.getNetWorkTime());
    }

    @Test
    void testGenerateMonthlyReport_MissingClockOut() {
        List<AttendanceRecord> records = List.of(
                createRecord(8, 0, AttendanceRecord.AttendanceType.CLOCK_IN)
        );

        when(attendanceRepository.findByEmployeeOrderByTimestampDesc(testEmployee)).thenReturn(records);

        List<AttendanceReportDto> report = attendanceService.generateMonthlyReport(testEmployee, testYear, testMonth);

        assertEquals(1, report.size());
        AttendanceReportDto dayReport = report.get(0);

        assertNotNull(dayReport.getClockOut());
        assertEquals(LocalDateTime.of(testYear, testMonth, 15, 20, 30), dayReport.getClockOut());

        assertEquals(Duration.ofMinutes(30), dayReport.getTotalBreakTime());
        assertEquals(Duration.ofHours(12), dayReport.getNetWorkTime());

        assertTrue(dayReport.isMissingClockOut());
        assertTrue(dayReport.isCappedAtMax());
    }

    @Test
    void testGenerateMonthlyReport_ShiftOver12AndHalfHours_Capped() {
        // Směna trvající 14 hodin (od 8:00 do 22:00)
        List<AttendanceRecord> records = Arrays.asList(
                createRecord(8, 0, AttendanceRecord.AttendanceType.CLOCK_IN),
                createRecord(22, 0, AttendanceRecord.AttendanceType.CLOCK_OUT)
        );

        when(attendanceRepository.findByEmployeeOrderByTimestampDesc(testEmployee)).thenReturn(records);

        List<AttendanceReportDto> report = attendanceService.generateMonthlyReport(testEmployee, testYear, testMonth);

        assertEquals(1, report.size());
        AttendanceReportDto dayReport = report.get(0);

        assertEquals(LocalDateTime.of(testYear, testMonth, 15, 20, 30), dayReport.getClockOut());

        assertEquals(Duration.ofMinutes(30), dayReport.getTotalBreakTime());
        assertEquals(Duration.ofHours(12), dayReport.getNetWorkTime());

        assertFalse(dayReport.isMissingClockOut());
        assertTrue(dayReport.isCappedAtMax());
    }

    private AttendanceRecord createRecord(int hour, int minute, AttendanceRecord.AttendanceType type) {
        return AttendanceRecord.builder()
                .employee(testEmployee)
                .type(type)
                .timestamp(LocalDateTime.of(testYear, testMonth, 15, hour, minute))
                .build();
    }
}