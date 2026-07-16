package org.example.controller;

import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.OrderStatusHistory;
import org.example.model.User;
import org.example.repository.OrderRepository;
import org.example.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Controller
@RequestMapping("/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminController {

    private final OrderRepository orderRepository;
    private final OrderStatusService orderStatusService;
    private final ProductService productService;
    private final UserService userService;
    private final AuditService auditService;
    private final LocalInvoiceService localInvoiceService;
    private final CouponService couponService;
    private final TaxRateService taxRateService;

    @GetMapping
    public String dashboard(Model model) {
        model.addAttribute("totalOrders", orderRepository.count());
        model.addAttribute("totalProducts", productService.count());

        model.addAttribute("recentOrders", orderRepository.findAll(
                PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt"))
        ).getContent());

        BigDecimal totalRevenue = orderRepository.sumTotalRevenue();
        model.addAttribute("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        model.addAttribute("totalUsers", userService.count());
        model.addAttribute("activeCouponsCount", couponService.countActive());
        model.addAttribute("taxRatesCount", taxRateService.count());

        return "admin/dashboard";
    }

    @GetMapping("/objednavky")
    public String orderList(Model model) {
        model.addAttribute("orders", orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt")));
        return "admin/objednavky";
    }

    @GetMapping("/objednavky/{id}")
    public String orderDetail(@PathVariable Long id, Model model) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Objednávka nenalezena"));
        model.addAttribute("order", order);
        model.addAttribute("allStatuses", orderStatusService.getAllOrdered());
        return "admin/objednavka-detail";
    }

    @PostMapping("/objednavky/status")
    public String updateOrderStatus(@RequestParam Long orderId,
                                    @RequestParam Long statusId,
                                    @RequestParam(required = false) String note,
                                    Principal principal,
                                    RedirectAttributes ra) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        OrderStatus newStatus = orderStatusService.findById(statusId);
        User currentUser = (principal != null) ? userService.findByEmail(principal.getName()).orElse(null) : null;

        order.setStatus(newStatus);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(newStatus)
                .note(note)
                .changedBy(currentUser)
                .createdAt(LocalDateTime.now())
                .build();

        order.getStatusHistory().add(history);
        orderRepository.save(order);

        auditService.log("OBJEDNÁVKY", "ZMENA_STAVU",
                "Stav objednávky " + order.getOrderNumber() + " změněn na: " + newStatus.getName());

        ra.addFlashAttribute("success", "Stav objednávky byl úspěšně změněn.");
        return "redirect:/admin/objednavky/" + orderId;
    }

    @GetMapping("/objednavky/{id}/export-excel")
    public ResponseEntity<InputStreamResource> exportInvoiceExcel(@PathVariable Long id) throws IOException {
        Order order = orderRepository.findById(id).orElseThrow();
        ByteArrayInputStream in = localInvoiceService.exportInvoiceToExcel(order);

        String filename = "Faktura_" + order.getOrderNumber() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/objednavky/export-excel")
    public ResponseEntity<InputStreamResource> exportAllOrdersExcel() throws IOException {
        List<Order> orders = orderRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        ByteArrayInputStream in = localInvoiceService.exportOrdersToExcel(orders);

        String filename = "Export_Objednavek_" + LocalDate.now().toString() + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

    @GetMapping("/logs")
    public String auditLogs(@RequestParam(required = false) Long userId,
                            @RequestParam(required = false) String module,
                            Model model) {
        model.addAttribute("logs", auditService.getFilteredLogs(userId, module));
        model.addAttribute("users", userService.findAll());
        model.addAttribute("modules", auditService.getAllModules());
        model.addAttribute("selectedUserId", userId);
        model.addAttribute("selectedModule", module);

        return "admin/audit-logs";
    }
}