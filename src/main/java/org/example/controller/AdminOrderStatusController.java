package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.OrderStatus;
import org.example.service.OrderStatusService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/nastaveni/stavy")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminOrderStatusController {

    private final OrderStatusService statusService;

    @GetMapping
    public String listStatuses(Model model) {
        model.addAttribute("statuses", statusService.getAllOrdered());
        model.addAttribute("newStatus", new OrderStatus());
        return "admin/order-statuses";
    }

    @PostMapping("/ulozit")
    public String saveStatus(@ModelAttribute OrderStatus orderStatus, RedirectAttributes ra) {
        statusService.save(orderStatus);
        ra.addFlashAttribute("success", "Stav objednávky byl úspěšně uložen.");
        return "redirect:/admin/nastaveni/stavy";
    }

    @PostMapping("/toggle-status/{id}")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            statusService.toggleActive(id);
            ra.addFlashAttribute("success", "Dostupnost stavu byla změněna.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Při změně stavu došlo k chybě.");
        }
        return "redirect:/admin/nastaveni/stavy";
    }
}