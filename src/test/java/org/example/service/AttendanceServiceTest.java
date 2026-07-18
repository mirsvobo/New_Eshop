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
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AttendanceRecordRepository attendanceRepository;

    @Mock
    private AuditService auditService;

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
        assertEquals(Duration.ofMinutes(30), dayReport.getTotalBreakTime());
        assertTrue(dayReport.isMissingClockOut());
        assertTrue(dayReport.isCappedAtMax());
    }

    @Test
    void testGenerateMonthlyReport_ShiftOver12AndHalfHours_Capped() {
        List<AttendanceRecord> records = Arrays.asList(
                createRecord(8, 0, AttendanceRecord.AttendanceType.CLOCK_IN),
                createRecord(22, 0, AttendanceRecord.AttendanceType.CLOCK_OUT)
        );
        when(attendanceRepository.findByEmployeeOrderByTimestampDesc(testEmployee)).thenReturn(records);

        List<AttendanceReportDto> report = attendanceService.generateMonthlyReport(testEmployee, testYear, testMonth);

        assertEquals(1, report.size());
        AttendanceReportDto dayReport = report.get(0);
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

    @Test
    void testFindAll() {
        when(attendanceRepository.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(java.util.Collections.emptyList());
        java.util.List<AttendanceRecord> records = attendanceService.findAll();
        assertNotNull(records);
        verify(attendanceRepository).findAll(any(org.springframework.data.domain.Sort.class));
    }

    @Test
    void testFindById() {
        AttendanceRecord record = new AttendanceRecord();
        record.setId(1L);
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(record));
        Optional<AttendanceRecord> found = attendanceService.findById(1L);
        assertTrue(found.isPresent());
        assertEquals(1L, found.get().getId());
    }

    @Test
    void testAuthenticateByPin_Success() {
        when(userRepository.findByPin("1234")).thenReturn(Optional.of(testEmployee));
        Optional<User> authUser = attendanceService.authenticateByPin("1234");
        assertTrue(authUser.isPresent());
        assertEquals(testEmployee, authUser.get());
    }

    @Test
    void testAuthenticateByPin_Customer_ReturnsEmpty() {
        User customer = User.builder().role(User.Role.ROLE_CUSTOMER).pin("5678").build();
        when(userRepository.findByPin("5678")).thenReturn(Optional.of(customer));
        Optional<User> authUser = attendanceService.authenticateByPin("5678");
        assertFalse(authUser.isPresent(), "Zákazník se nesmí přihlásit do terminálu.");
    }

    @Test
    void testGetCurrentStatus_Offline() {
        when(attendanceRepository.findByEmployeeOrderByTimestampDesc(testEmployee)).thenReturn(java.util.Collections.emptyList());
        AttendanceService.WorkerStatus status = attendanceService.getCurrentStatus(testEmployee);
        assertEquals(AttendanceService.WorkerStatus.OFFLINE, status);
    }

    @Test
    void testGetCurrentStatus_Working() {
        AttendanceRecord record = AttendanceRecord.builder().type(AttendanceRecord.AttendanceType.CLOCK_IN).build();
        when(attendanceRepository.findByEmployeeOrderByTimestampDesc(testEmployee)).thenReturn(java.util.List.of(record));
        AttendanceService.WorkerStatus status = attendanceService.getCurrentStatus(testEmployee);
        assertEquals(AttendanceService.WorkerStatus.WORKING, status);
    }

    @Test
    void testRecordAttendance() {
        attendanceService.recordAttendance(testEmployee, AttendanceRecord.AttendanceType.CLOCK_IN);
        verify(attendanceRepository).save(any(AttendanceRecord.class));
        verify(auditService).log(eq("DOCHÁZKA"), eq("Záznam docházky"), anyString());
    }

    @Test
    void testSave() {
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployee(testEmployee);
        attendanceService.save(record);
        verify(attendanceRepository).save(record);
        verify(auditService).log(eq("DOCHÁZKA"), eq("Ruční přidání"), anyString());
    }

    @Test
    void testDelete() {
        AttendanceRecord record = new AttendanceRecord();
        record.setEmployee(testEmployee);
        when(attendanceRepository.findById(1L)).thenReturn(Optional.of(record));

        attendanceService.delete(1L);

        verify(attendanceRepository).deleteById(1L);
        verify(auditService).log(eq("DOCHÁZKA"), eq("Smazání"), anyString());
    }
}