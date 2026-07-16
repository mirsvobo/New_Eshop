package org.example.controller;

import org.example.dto.AttendanceReportDto;
import org.example.model.AttendanceRecord;
import org.example.model.User;
import org.example.service.AttendanceService;
import org.example.service.AuditService;
import org.example.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.Principal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Controller
@RequestMapping("/dochazka")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserService userService;
    private final AuditService auditService;

    @GetMapping
    public String terminalPage() { return "attendance"; }

    @PostMapping("/api/verify")
    @ResponseBody
    public ResponseEntity<?> verifyPin(@RequestBody Map<String, String> payload) {
        String pin = payload.get("pin");
        if (pin != null) pin = pin.trim();
        Optional<User> userOpt = attendanceService.authenticateByPin(pin);
        if (userOpt.isPresent()) {
            User user = userOpt.get();
            AttendanceService.WorkerStatus status = attendanceService.getCurrentStatus(user);
            return ResponseEntity.ok(Map.of("success", true, "worker", Map.of("name", user.getFullName(), "status", status.name().toLowerCase())));
        }
        return ResponseEntity.status(401).body(Map.of("success", false, "message", "Zadaný PIN neexistuje."));
    }

    @PostMapping("/api/action")
    @ResponseBody
    public ResponseEntity<?> performAction(@RequestBody Map<String, String> payload) {
        String pin = payload.get("pin");
        if (pin != null) pin = pin.trim();
        String actionStr = payload.get("action");
        Optional<User> userOpt = attendanceService.authenticateByPin(pin);
        if (userOpt.isPresent()) {
            attendanceService.recordAttendance(userOpt.get(), mapActionToType(actionStr));
            return ResponseEntity.ok(Map.of("success", true, "message", "Záznam byl uložen."));
        }
        return ResponseEntity.status(401).body(Map.of("success", false, "message", "Neautorizovaný přístup."));
    }

    @GetMapping("/admin")
    public String listLogs(@RequestParam(required = false) Long employeeId,
                           @RequestParam(required = false) AttendanceRecord.AttendanceType type,
                           @RequestParam(required = false) String dateRange,
                           Principal principal,
                           Model model) {
        User currentUser = userService.findByEmail(principal.getName()).orElseThrow();
        boolean isAdmin = currentUser.getRole() == User.Role.ROLE_ADMIN;

        if (!isAdmin) {
            employeeId = currentUser.getId();
        }

        List<AttendanceRecord> filtered = attendanceService.getFilteredRecords(employeeId, type, dateRange);
        model.addAttribute("records", filtered);

        if (isAdmin) {
            model.addAttribute("employees", userService.findAll().stream().filter(u -> u.getRole() != User.Role.ROLE_CUSTOMER).toList());
        } else {
            model.addAttribute("employees", List.of(currentUser));
        }

        model.addAttribute("types", AttendanceRecord.AttendanceType.values());
        model.addAttribute("summary", attendanceService.calculateSummary(filtered));
        model.addAttribute("selectedEmployee", employeeId);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedDate", dateRange);
        return "admin/attendance-logs";
    }

    @GetMapping("/admin/report")
    public String showReport(@RequestParam(required = false) Long userId,
                             @RequestParam(required = false) Integer year,
                             @RequestParam(required = false) Integer month,
                             Principal principal,
                             Model model) {
        User currentUser = userService.findByEmail(principal.getName()).orElseThrow();
        boolean isAdmin = currentUser.getRole() == User.Role.ROLE_ADMIN;

        int targetYear = (year != null) ? year : LocalDate.now().getYear();
        int targetMonth = (month != null) ? month : LocalDate.now().getMonthValue();
        model.addAttribute("selectedYear", targetYear);
        model.addAttribute("selectedMonth", targetMonth);

        if (!isAdmin) {
            userId = currentUser.getId();
            model.addAttribute("users", List.of(currentUser));
        } else {
            model.addAttribute("users", userService.findAll().stream().filter(u -> u.getRole() != User.Role.ROLE_CUSTOMER).toList());
        }

        if (userId != null) {
            User employee = userService.findById(userId).orElseThrow();

            if (!isAdmin && !employee.getId().equals(currentUser.getId())) {
                employee = currentUser;
            }

            List<AttendanceReportDto> reportData = attendanceService.generateMonthlyReport(employee, targetYear, targetMonth);
            model.addAttribute("report", reportData);
            model.addAttribute("selectedUser", employee);
            Duration totalNet = reportData.stream().map(AttendanceReportDto::getNetWorkTime).reduce(Duration.ZERO, Duration::plus);
            model.addAttribute("totalSummary", String.format("%d hod %d min", totalNet.toHours(), totalNet.toMinutesPart()));
        }
        return "admin/attendance-report";
    }

    @GetMapping("/admin/report/export")
    public ResponseEntity<InputStreamResource> exportReport(@RequestParam Long userId,
                                                            @RequestParam Integer year,
                                                            @RequestParam Integer month,
                                                            Principal principal) throws IOException {
        User currentUser = userService.findByEmail(principal.getName()).orElseThrow();
        boolean isAdmin = currentUser.getRole() == User.Role.ROLE_ADMIN;

        if (!isAdmin && !userId.equals(currentUser.getId())) {
            return ResponseEntity.status(403).build();
        }

        User employee = userService.findById(userId).orElseThrow();
        ByteArrayInputStream in = attendanceService.exportMonthlyReportToExcel(employee, year, month);

        String filename = String.format("Dochazka_%s_%d_%d.xlsx", employee.getLastName(), month, year);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/admin/novy")
    public String manualEntryForm(Model model, Principal principal) {
        User currentUser = userService.findByEmail(principal.getName()).orElseThrow();
        if (currentUser.getRole() != User.Role.ROLE_ADMIN) return "redirect:/dochazka/admin";

        AttendanceRecord record = new AttendanceRecord();
        record.setTimestamp(LocalDateTime.now());
        model.addAttribute("record", record);
        model.addAttribute("users", userService.findAll().stream().filter(u -> u.getRole() != User.Role.ROLE_CUSTOMER).toList());
        model.addAttribute("types", AttendanceRecord.AttendanceType.values());
        return "admin/attendance-form";
    }

    @GetMapping("/admin/edit/{id}")
    public String editEntryForm(@PathVariable Long id, Model model, Principal principal) {
        User currentUser = userService.findByEmail(principal.getName()).orElseThrow();
        if (currentUser.getRole() != User.Role.ROLE_ADMIN) return "redirect:/dochazka/admin";

        model.addAttribute("record", attendanceService.findById(id).orElseThrow());
        model.addAttribute("users", userService.findAll().stream().filter(u -> u.getRole() != User.Role.ROLE_CUSTOMER).toList());
        model.addAttribute("types", AttendanceRecord.AttendanceType.values());
        return "admin/attendance-form";
    }

    @PostMapping("/admin/save")
    public String saveEntry(@ModelAttribute AttendanceRecord record, Principal principal, RedirectAttributes ra) {
        User currentUser = userService.findByEmail(principal.getName()).orElseThrow();
        if (currentUser.getRole() != User.Role.ROLE_ADMIN) return "redirect:/dochazka/admin";

        attendanceService.save(record);

        auditService.log("DOCHÁZKA", "MANUÁLNÍ_ZÁSAH", "Upraven záznam pro: " + record.getEmployee().getFullName());
        ra.addFlashAttribute("success", "Záznam byl uložen.");
        return "redirect:/dochazka/admin";
    }

    @PostMapping("/admin/delete/{id}")
    public String deleteEntry(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        User currentUser = userService.findByEmail(principal.getName()).orElseThrow();
        if (currentUser.getRole() != User.Role.ROLE_ADMIN) return "redirect:/dochazka/admin";

        attendanceService.findById(id).ifPresent(r -> {
            attendanceService.delete(id);
            auditService.log("DOCHÁZKA", "SMAZÁN_ZÁZNAM", "Smazána událost: " + r.getEmployee().getFullName());
        });
        ra.addFlashAttribute("success", "Záznam odstraněn.");
        return "redirect:/dochazka/admin";
    }

    private AttendanceRecord.AttendanceType mapActionToType(String action) {
        return switch (action) {
            case "clock_in" -> AttendanceRecord.AttendanceType.CLOCK_IN;
            case "clock_out" -> AttendanceRecord.AttendanceType.CLOCK_OUT;
            case "break_start" -> AttendanceRecord.AttendanceType.BREAK_START;
            case "break_end" -> AttendanceRecord.AttendanceType.BREAK_END;
            default -> null;
        };
    }
}