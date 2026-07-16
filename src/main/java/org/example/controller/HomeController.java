package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.Product;
import org.example.repository.ProductRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.stream.Collectors;


@Controller
@RequiredArgsConstructor
public class HomeController {

    private final ProductRepository productRepository;

    @GetMapping("/")
    public String home(Model model) {
        List<Product> featuredProducts = productRepository.findAll().stream()
                .filter(Product::isActive)
                .filter(p -> p.getType() == Product.ProductType.PRODUCT)
                .limit(4)
                .collect(Collectors.toList());

        model.addAttribute("featuredProducts", featuredProducts);
        return "index";
    }

    @GetMapping("/o-nas")
    public String aboutUs() {
        return "o-nas";
    }

    @GetMapping("/kontakt")
    public String contact() {
        return "kontakt";
    }
}