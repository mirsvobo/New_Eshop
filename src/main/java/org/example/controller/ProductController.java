package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.Product;
import org.example.repository.ProductRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping("/produkty")
    public String listProducts(@RequestParam(defaultValue = "newest") String sort, Model model) {
        Sort sortObj = switch (sort) {
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "name_asc" -> Sort.by(Sort.Direction.ASC, "name");
            default -> Sort.by(Sort.Direction.DESC, "id");
        };

        List<Product> products = productRepository.findByActiveTrueAndType(Product.ProductType.PRODUCT, sortObj);

        model.addAttribute("products", products);
        model.addAttribute("currentSort", sort);
        return "produkty";
    }

    @GetMapping("/produkty/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Produkt nenalezen"));

        if (!product.isActive() || product.getType() == Product.ProductType.MATERIAL) {
            return "redirect:/produkty";
        }

        model.addAttribute("product", product);
        return "produkt-detail";
    }
}