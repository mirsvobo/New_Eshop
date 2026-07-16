package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.Product;
import org.example.model.StockMovement;
import org.example.model.User;
import org.example.repository.ProductRepository;
import org.example.repository.UserRepository;
import org.example.service.InventoryService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.security.Principal;

@Controller
@RequestMapping("/admin/sklad")
@RequiredArgsConstructor
public class InventoryController {

    private final InventoryService inventoryService;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @GetMapping
    public String viewSklad(
            @RequestParam(required = false) Long productId,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String direction,
            @RequestParam(required = false) Double minQty,
            @RequestParam(required = false) Double maxQty,
            @RequestParam(required = false) String dateRange,
            Model model) {

        model.addAttribute("movements", inventoryService.getFilteredMovements(
                productId, userId, direction, minQty, maxQty, dateRange));

        model.addAttribute("materials", productRepository.findAll().stream()
                .filter(p -> p.getType() == Product.ProductType.MATERIAL).toList());
        model.addAttribute("finalProducts", productRepository.findAll().stream()
                .filter(p -> p.getType() == Product.ProductType.PRODUCT).toList());

        model.addAttribute("users", userRepository.findAll());

        model.addAttribute("manualTypes", StockMovement.MovementType.values());

        model.addAttribute("selectedProduct", productId);
        model.addAttribute("selectedUser", userId);
        model.addAttribute("selectedDirection", direction);
        model.addAttribute("minQty", minQty);
        model.addAttribute("maxQty", maxQty);
        model.addAttribute("selectedDate", dateRange);

        return "admin/sklad";
    }

    @PostMapping("/pridat")
    public String addMovement(@RequestParam Long productId,
                              @RequestParam double quantity,
                              @RequestParam StockMovement.MovementType type,
                              @RequestParam(required = false) String note,
                              Principal principal,
                              RedirectAttributes ra) {
        try {
            User user = userRepository.findByEmail(principal.getName()).orElseThrow();
            inventoryService.recordMovement(productId, (int) quantity, type, note, user);
            ra.addFlashAttribute("success", "Skladový pohyb byl úspěšně zaevidován.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Chyba při ukládání pohybu: " + e.getMessage());
        }
        return "redirect:/admin/sklad";
    }

    @PostMapping("/vyroba")
    public String produceProduct(@RequestParam Long productId,
                                 @RequestParam double quantity,
                                 Principal principal,
                                 RedirectAttributes ra) {
        try {
            User user = userRepository.findByEmail(principal.getName()).orElseThrow();
            inventoryService.produceProduct(productId, quantity, user);
            ra.addFlashAttribute("success", "Produkt byl úspěšně zadán do výroby.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Chyba při výrobě: " + e.getMessage());
        }
        return "redirect:/admin/sklad";
    }
}