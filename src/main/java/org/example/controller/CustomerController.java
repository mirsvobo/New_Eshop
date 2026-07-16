package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.Order;
import org.example.model.User;
import org.example.repository.OrderRepository;
import org.example.repository.UserRepository;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.security.Principal;
import java.util.List;

@Controller
@RequiredArgsConstructor
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerController {

    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    @GetMapping("/muj-ucet")
    public String myAccount(Principal principal, Model model) {
        User user = userRepository.findByEmail(principal.getName()).orElseThrow();
        List<Order> orders = orderRepository.findByCustomerOrderByCreatedAtDesc(user);

        model.addAttribute("user", user);
        model.addAttribute("orders", orders);
        return "muj-ucet";
    }

    @GetMapping("/muj-ucet/objednavka/{id}")
    public String customerOrderDetail(@PathVariable Long id, Principal principal, Model model) {
        User customer = userRepository.findByEmail(principal.getName()).orElseThrow();
        Order order = orderRepository.findById(id).orElseThrow();

        if (order.getCustomer() == null || !order.getCustomer().getId().equals(customer.getId())) {
            return "redirect:/muj-ucet";
        }

        model.addAttribute("order", order);
        return "objednavka-detail-zakaznik";
    }
}