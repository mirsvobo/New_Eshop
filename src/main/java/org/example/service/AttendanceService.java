package org.example.service;

import jakarta.persistence.criteria.Predicate;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.example.dto.AttendanceReportDto;
import org.example.model.AttendanceRecord;
import org.example.model.User;
import org.example.repository.AttendanceRecordRepository;
import org.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.DayOfWeek;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAdjusters;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AttendanceService {

    private final UserRepository userRepository;
    private final AttendanceRecordRepository attendanceRepository;
    private final AuditService auditService;

    private static final String MODULE_NAME = "DOCHÁZKA";
    private static final Duration MANDATORY_BREAK = Duration.ofMinutes(30);
    private static final Duration SHIFT_THRESHOLD_FOR_BREAK = Duration.ofHours(6);
    private static final Duration MAX_SHIFT_DURATION = Duration.ofMinutes(750);

    public List<AttendanceRecord> findAll() {
        return attendanceRepository.findAll(Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    public List<AttendanceRecord> getFilteredRecords(Long employeeId, AttendanceRecord.AttendanceType type, String dateRange) {
        return attendanceRepository.findAll((Specification<AttendanceRecord>) (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (employeeId != null) predicates.add(cb.equal(root.get("employee").get("id"), employeeId));
            if (type != null) predicates.add(cb.equal(root.get("type"), type));
            if (dateRange != null && !dateRange.isEmpty()) {
                LocalDateTime now = LocalDateTime.now();
                switch (dateRange) {
                    case "today" ->
                            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), now.toLocalDate().atStartOfDay()));
                    case "wtd" ->
                            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), now.with(DayOfWeek.MONDAY).toLocalDate().atStartOfDay()));
                    case "mtd" ->
                            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), now.with(TemporalAdjusters.firstDayOfMonth()).toLocalDate().atStartOfDay()));
                    case "ytd" ->
                            predicates.add(cb.greaterThanOrEqualTo(root.get("timestamp"), now.with(TemporalAdjusters.firstDayOfYear()).toLocalDate().atStartOfDay()));
                }
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        }, Sort.by(Sort.Direction.DESC, "timestamp"));
    }

    public Map<String, Object> calculateSummary(List<AttendanceRecord> records) {
        Map<LocalDate, Map<User, List<AttendanceRecord>>> grouped = records.stream()
                .collect(Collectors.groupingBy(r -> r.getTimestamp().toLocalDate(), Collectors.groupingBy(AttendanceRecord::getEmployee)));
        Duration totalDuration = Duration.ZERO;
        int activeWorkerDays = 0;
        for (var dayEntry : grouped.values()) {
            for (var userEntry : dayEntry.values()) {
                AttendanceReportDto daily = calculateDailyAttendance(null, userEntry);
                totalDuration = totalDuration.plus(daily.getNetWorkTime());
                activeWorkerDays++;
            }
        }
        long clockInCount = records.stream().filter(r -> r.getType() == AttendanceRecord.AttendanceType.CLOCK_IN).count();
        return Map.of(
                "totalHours", String.format("%d hod %d min", totalDuration.toHours(), totalDuration.toMinutesPart()),
                "avgHours", activeWorkerDays > 0 ? String.format("%.1f hod", (double) totalDuration.toMinutes() / 60 / activeWorkerDays) : "0",
                "entryCount", records.size(),
                "clockInCount", clockInCount
        );
    }

    public ByteArrayInputStream exportMonthlyReportToExcel(User employee, int year, int month) throws IOException {
        List<AttendanceReportDto> reportData = generateMonthlyReport(employee, year, month);

        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Docházka " + month + "-" + year);

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            Row headerRow = sheet.createRow(0);
            String[] columns = {"Datum", "Příchod", "Odchod", "Pauza (min)", "Čistý čas", "Poznámka"};
            for (int i = 0; i < columns.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(columns[i]);
                cell.setCellStyle(headerStyle);
            }

            int rowIdx = 1;
            Duration totalNet = Duration.ZERO;
            DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm");

            for (AttendanceReportDto dto : reportData) {
                Row row = sheet.createRow(rowIdx++);
                row.createCell(0).setCellValue(dto.getDate().toString());
                row.createCell(1).setCellValue(dto.getClockIn() != null ? dto.getClockIn().format(timeFormatter) : "-");

                String clockOutStr = "-";
                if (dto.getClockOut() != null) {
                    clockOutStr = dto.getClockOut().format(timeFormatter);
                    if (dto.isMissingClockOut()) clockOutStr += " (!)";
                }
                row.createCell(2).setCellValue(clockOutStr);

                row.createCell(3).setCellValue(dto.getTotalBreakTime().toMinutes());
                row.createCell(4).setCellValue(String.format("%d:%02d", dto.getNetWorkTime().toHours(), dto.getNetWorkTime().toMinutesPart()));

                String note = "";
                if (dto.isMissingClockOut()) note += "Chybí odchod; ";
                if (dto.isCappedAtMax()) note += "Aplikován limit 12,5h;";
                row.createCell(5).setCellValue(note);

                totalNet = totalNet.plus(dto.getNetWorkTime());
            }

            Row summaryRow = sheet.createRow(rowIdx + 1);
            Cell sumLabel = summaryRow.createCell(3);
            sumLabel.setCellValue("CELKEM:");
            sumLabel.setCellStyle(headerStyle);

            summaryRow.createCell(4).setCellValue(String.format("%d hod %d min", totalNet.toHours(), totalNet.toMinutesPart()));

            for (int i = 0; i < columns.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(out);
            return new ByteArrayInputStream(out.toByteArray());
        }
    }

    public List<AttendanceReportDto> generateMonthlyReport(User employee, int year, int month) {
        List<AttendanceRecord> records = attendanceRepository.findByEmployeeOrderByTimestampDesc(employee).stream()
                .filter(r -> r.getTimestamp().getYear() == year && r.getTimestamp().getMonthValue() == month)
                .sorted(Comparator.comparing(AttendanceRecord::getTimestamp))
                .toList();
        Map<LocalDate, List<AttendanceRecord>> groupedByDay = records.stream()
                .collect(Collectors.groupingBy(r -> r.getTimestamp().toLocalDate()));
        return groupedByDay.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> calculateDailyAttendance(entry.getKey(), entry.getValue()))
                .toList();
    }

    private AttendanceReportDto calculateDailyAttendance(LocalDate date, List<AttendanceRecord> dayRecords) {
        LocalDateTime firstIn = null;
        LocalDateTime lastOut = null;
        Duration breakDuration = Duration.ZERO;
        LocalDateTime breakStart = null;

        List<AttendanceRecord> sorted = dayRecords.stream().sorted(Comparator.comparing(AttendanceRecord::getTimestamp)).toList();
        for (AttendanceRecord r : sorted) {
            switch (r.getType()) {
                case CLOCK_IN -> {
                    if (firstIn == null) firstIn = r.getTimestamp();
                }
                case CLOCK_OUT -> lastOut = r.getTimestamp();
                case BREAK_START -> breakStart = r.getTimestamp();
                case BREAK_END -> {
                    if (breakStart != null) {
                        breakDuration = breakDuration.plus(Duration.between(breakStart, r.getTimestamp()));
                        breakStart = null;
                    }
                }
            }
        }

        boolean missingClockOut = false;
        boolean cappedAtMax = false;
        LocalDateTime effectiveOut = lastOut;

        if (firstIn != null && lastOut == null) {
            missingClockOut = true;
            effectiveOut = LocalDateTime.now();
        }

        Duration totalTime = (firstIn != null && effectiveOut != null) ? Duration.between(firstIn, effectiveOut) : Duration.ZERO;

        if (totalTime.compareTo(MAX_SHIFT_DURATION) > 0) {
            totalTime = MAX_SHIFT_DURATION;
            cappedAtMax = true;
            effectiveOut = firstIn.plus(MAX_SHIFT_DURATION);
        }

        if (totalTime.compareTo(SHIFT_THRESHOLD_FOR_BREAK) > 0 && breakDuration.compareTo(MANDATORY_BREAK) < 0) {
            breakDuration = MANDATORY_BREAK;
        }

        Duration netTime = totalTime.minus(breakDuration);

        return AttendanceReportDto.builder()
                .date(date)
                .clockIn(firstIn)
                .clockOut(effectiveOut)
                .totalBreakTime(breakDuration)
                .netWorkTime(netTime.isNegative() ? Duration.ZERO : netTime)
                .missingClockOut(missingClockOut)
                .cappedAtMax(cappedAtMax)
                .build();
    }

    public Optional<AttendanceRecord> findById(Long id) {
        return attendanceRepository.findById(id);
    }

    public Optional<User> authenticateByPin(String pin) {
        return userRepository.findByPin(pin).filter(u -> u.getRole() != User.Role.ROLE_CUSTOMER);
    }

    public WorkerStatus getCurrentStatus(User employee) {
        List<AttendanceRecord> records = attendanceRepository.findByEmployeeOrderByTimestampDesc(employee);
        if (records.isEmpty()) return WorkerStatus.OFFLINE;
        AttendanceRecord last = records.get(0);
        return switch (last.getType()) {
            case CLOCK_IN, BREAK_END -> WorkerStatus.WORKING;
            case BREAK_START -> WorkerStatus.BREAK;
            case CLOCK_OUT -> WorkerStatus.OFFLINE;
        };
    }

    @Transactional
    public void recordAttendance(User emp, AttendanceRecord.AttendanceType type) {
        attendanceRepository.save(AttendanceRecord.builder().employee(emp).type(type).timestamp(LocalDateTime.now()).build());
        auditService.log(MODULE_NAME, "Záznam docházky",
                "Zaměstnanec " + emp.getEmail() + " zaznamenal akci: " + type.name() + ".");
    }

    @Transactional
    public void save(AttendanceRecord r) {
        boolean isNew = (r.getId() == null);
        attendanceRepository.save(r);
        String action = isNew ? "Ruční přidání" : "Ruční úprava";
        auditService.log(MODULE_NAME, action,
                "Administrátor provedl " + action.toLowerCase() + " u záznamu zaměstnance ID " + r.getEmployee().getId() + ".");
    }

    @Transactional
    public void delete(Long id) {
        Optional<AttendanceRecord> record = attendanceRepository.findById(id);
        if (record.isPresent()) {
            String empEmail = record.get().getEmployee().getEmail();
            attendanceRepository.deleteById(id);
            auditService.log(MODULE_NAME, "Smazání", "Byl trvale smazán záznam docházky zaměstnance " + empEmail + ".");
        }
    }

    public enum WorkerStatus {WORKING, BREAK, OFFLINE}
}