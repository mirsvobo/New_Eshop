package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.InstallationPost;
import org.example.model.Product;
import org.example.repository.InstallationPostRepository;
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
    private final InstallationPostRepository installationPostRepository;

    @GetMapping("/")
    public String home(Model model) {

        // Odstraněno .limit(4), aby se načetly všechny produkty
        // a mohly se rozdělit do obou carouselů
        List<Product> featuredProducts = productRepository.findAll().stream()
                .filter(Product::isActive)
                .filter(product -> product.getType() == Product.ProductType.PRODUCT)
                .collect(Collectors.toList());

        List<InstallationPost> installationPosts =
                installationPostRepository.findAllByActiveTrueOrderByAssemblyDateDesc();

        model.addAttribute("featuredProducts", featuredProducts);
        model.addAttribute("installationPosts", installationPosts);

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