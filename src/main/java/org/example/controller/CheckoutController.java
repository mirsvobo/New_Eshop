package org.example.controller;

import jakarta.validation.Valid;
import org.example.dto.CheckoutFormDataDto;
import org.example.model.Order;
import org.example.model.TaxMode;
import org.example.model.User;
import org.example.repository.UserRepository;
import org.example.service.Cart;
import org.example.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;

@Controller
@RequestMapping("/kosik")
@RequiredArgsConstructor
public class CheckoutController {

    private final Cart cart;
    private final OrderService orderService;
    private final UserRepository userRepository;

    @GetMapping("/pokladna")
    public String checkoutForm(Model model, Principal principal) {
        if (cart.getItems().isEmpty()) return "redirect:/kosik";

        CheckoutFormDataDto formData = new CheckoutFormDataDto();

        // Přenese se aktuální režim z košíku do formuláře
        formData.setTaxMode(cart.getTaxMode());

        if (principal != null) {
            userRepository.findByEmail(principal.getName()).ifPresent(user -> {
                formData.setFirstName(user.getFirstName());
                formData.setLastName(user.getLastName());
                formData.setEmail(user.getEmail());
            });
        }

        model.addAttribute("checkoutForm", formData);
        model.addAttribute("cartItems", cart.getItems());
        model.addAttribute("cartTotal", cart.getTotalPrice());
        return "checkout";
    }

    @PostMapping("/objednat")
    public String processCheckout(@Valid @ModelAttribute("checkoutForm") CheckoutFormDataDto formData,
                                  BindingResult bindingResult, Principal principal, Model model) {

        if (!formData.isBusinessValidationOk()) {
            bindingResult.rejectValue("ico", "error.ico", "Při vyplnění IČO musí být vyplněno i DIČ a naopak.");
        }

        // Nové DPH validace dle požadavků
        if (formData.getTaxMode() == TaxMode.REDUCED) {
            if (!formData.isAffidavitSigned()) {
                bindingResult.rejectValue("affidavitSigned", "error.affidavitSigned", "Pro uplatnění 12% sazby DPH pro bydlení musíte podepsat čestné prohlášení.");
            }
            if (formData.getIco() != null && !formData.getIco().isBlank()) {
                bindingResult.rejectValue("ico", "error.ico", "Sníženou sazbu DPH pro bydlení nelze uplatnit při nákupu na IČO.");
            }
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("cartItems", cart.getItems());
            model.addAttribute("cartTotal", cart.getTotalPrice());
            return "checkout";
        }

        User customer = (principal != null) ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
        Order savedOrder = orderService.processCheckout(customer, formData, formData.getCouponCode());
        return "redirect:/kosik/potvrzeni/" + savedOrder.getOrderNumber();
    }

    @GetMapping("/potvrzeni/{orderNumber}")
    public String confirmation(@PathVariable String orderNumber, Model model) {
        model.addAttribute("orderNumber", orderNumber);
        return "potvrzeni";
    }
}