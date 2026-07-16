package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.Coupon;
import org.example.model.DiscountType;
import org.example.repository.CouponRepository;
import org.example.repository.ProductRepository;
import org.example.service.CouponService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin/coupons")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminCouponController {

    private final CouponRepository couponRepository;
    private final CouponService couponService;
    private final ProductRepository productRepository;

    @GetMapping("")
    public String listCoupons(Model model) {
        model.addAttribute("coupons", couponService.findAll());
        return "admin/coupon-list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("coupon", new Coupon());
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("products", productRepository.findAll());
        return "admin/coupon-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        model.addAttribute("coupon", couponService.findById(id));
        model.addAttribute("discountTypes", DiscountType.values());
        model.addAttribute("products", productRepository.findAll());
        return "admin/coupon-form";
    }

    @PostMapping("/save")
    public String saveCoupon(@ModelAttribute Coupon coupon, RedirectAttributes ra) {
        try {
            couponService.save(coupon);
            ra.addFlashAttribute("successMessage", "Kupón byl úspěšně uložen.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Při ukládání kupónu došlo k chybě: " + e.getMessage());
        }
        return "redirect:/admin/coupons";
    }

    @GetMapping("/delete/{id}")
    public String deleteCoupon(@PathVariable Long id, RedirectAttributes ra) {
        try {
            couponService.delete(id);
            ra.addFlashAttribute("successMessage", "Kupón byl smazán.");
        } catch (Exception e) {
            ra.addFlashAttribute("errorMessage", "Kupón nelze smazat.");
        }
        return "redirect:/admin/coupons";
    }
}