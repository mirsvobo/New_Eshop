package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.User;
import org.example.repository.OrderRepository;
import org.example.service.UserService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;

@Controller
@RequestMapping("/admin/uzivatele")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminUserController {

    private final UserService userService;
    private final OrderRepository orderRepository;

    @GetMapping
    public String listUsers(@RequestParam(required = false) User.Role roleFilter, Model model) {
        List<User> users = (roleFilter != null) ? userService.getUsersByRole(roleFilter) : userService.getAllUsers();
        model.addAttribute("users", users);
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("currentFilter", roleFilter);
        return "admin/user-list";
    }

    @GetMapping("/novy")
    public String showCreateForm(Model model) {
        User user = new User();
        user.setActive(true);
        user.setRole(User.Role.ROLE_CUSTOMER);
        model.addAttribute("user", user);
        model.addAttribute("roles", User.Role.values());
        return "admin/user-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        User user = userService.getUserById(id);

        BigDecimal totalSpent = orderRepository.sumTotalAmountByCustomer(user);
        if (totalSpent == null) totalSpent = BigDecimal.ZERO;

        model.addAttribute("user", user);
        model.addAttribute("roles", User.Role.values());
        model.addAttribute("orders", orderRepository.findByCustomerOrderByCreatedAtDesc(user));
        model.addAttribute("totalSpent", totalSpent);

        return "admin/user-form";
    }

    @PostMapping("/ulozit")
    public String saveUser(@ModelAttribute User user,
                           @RequestParam(required = false) String rawPassword,
                           RedirectAttributes ra) {
        try {
            userService.saveUser(user, rawPassword);
            ra.addFlashAttribute("success", "Uživatel byl úspěšně uložen.");
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("posledního")) return "redirect:/admin/uzivatele/prevod-prav";
            ra.addFlashAttribute("error", e.getMessage());
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Chyba při ukládání uživatele.");
        }
        return "redirect:/admin/uzivatele";
    }

    @PostMapping("/zmenit-stav/{id}")
    public String toggleStatus(@PathVariable Long id, RedirectAttributes ra) {
        try {
            userService.toggleUserStatus(id);
            ra.addFlashAttribute("success", "Stav uživatele byl změněn.");
        } catch (IllegalStateException e) {
            if (e.getMessage().contains("posledního")) return "redirect:/admin/uzivatele/prevod-prav";
            ra.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/admin/uzivatele";
    }

    @GetMapping("/prevod-prav")
    public String showTransferAdminRightsForm(Model model) {
        model.addAttribute("candidates", userService.getActiveEmployees());
        return "admin/transfer-admin";
    }

    @PostMapping("/prevod-prav")
    public String transferAdminRights(@RequestParam Long newAdminId, RedirectAttributes ra) {
        try {
            userService.promoteToAdmin(newAdminId);
            ra.addFlashAttribute("success", "Administrátorská práva byla úspěšně předána.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Při převádění práv došlo k chybě.");
        }
        return "redirect:/admin/uzivatele";
    }
}