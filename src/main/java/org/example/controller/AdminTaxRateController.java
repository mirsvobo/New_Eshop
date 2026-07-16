package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.TaxRate;
import org.example.service.TaxRateService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/tax-rates")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminTaxRateController {

    private final TaxRateService taxRateService;

    @GetMapping
    public String listTaxRates(Model model) {
        model.addAttribute("taxRates", taxRateService.findAll());
        return "admin/tax-rates";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("taxRate", new TaxRate());
        return "admin/tax-form";
    }

    @PostMapping("/save")
    public String saveTaxRate(@ModelAttribute TaxRate taxRate, RedirectAttributes ra) {
        try {
            taxRateService.save(taxRate);
            ra.addFlashAttribute("success", "Daňová sazba byla úspěšně uložena.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Chyba při ukládání sazby: " + e.getMessage());
        }
        return "redirect:/admin/tax-rates";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("taxRate", taxRateService.findById(id));
        return "admin/tax-form";
    }

    @GetMapping("/delete/{id}")
    public String deleteTaxRate(@PathVariable Long id, RedirectAttributes ra) {
        try {
            taxRateService.delete(id);
            ra.addFlashAttribute("success", "Daňová sazba byla odstraněna.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Sazbu nelze smazat, pravděpodobně je navázána na existující produkty.");
        }
        return "redirect:/admin/tax-rates";
    }
}