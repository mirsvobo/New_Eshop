package org.example.controller;

import org.example.model.Product;
import org.example.model.TaxRate;
import org.example.model.User;
import org.example.repository.ProductRepository;
import org.example.repository.TaxRateRepository;
import org.example.repository.UserRepository;
import org.example.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import jakarta.validation.Valid;

import java.security.Principal;
import java.util.List;

@Controller
@RequestMapping("/admin/produkty")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminProductController {

    private final ProductRepository productRepository;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final TaxRateRepository taxRateRepository;

    @GetMapping("")
    public String listProducts(
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Product.ProductType type,
            @RequestParam(required = false) Boolean active,
            @RequestParam(required = false, defaultValue = "name_asc") String sort,
            Model model) {

        Sort sortObj = switch (sort) {
            case "name_desc" -> Sort.by(Sort.Direction.DESC, "name");
            case "price_asc" -> Sort.by(Sort.Direction.ASC, "price");
            case "price_desc" -> Sort.by(Sort.Direction.DESC, "price");
            case "stock_asc" -> Sort.by(Sort.Direction.ASC, "stockQuantity");
            case "stock_desc" -> Sort.by(Sort.Direction.DESC, "stockQuantity");
            default -> Sort.by(Sort.Direction.ASC, "name");
        };

        model.addAttribute("products", productRepository.findFilteredProducts(search, type, active, sortObj));
        model.addAttribute("currentSearch", search);
        model.addAttribute("currentType", type);
        model.addAttribute("currentActive", active);
        model.addAttribute("currentSort", sort);
        model.addAttribute("productTypes", Product.ProductType.values());

        return "admin/produkty";
    }

    @GetMapping("/novy")
    public String showAddProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("taxes", taxRateRepository.findAll());
        return "admin/product-form";
    }

    @GetMapping("/edit/{id}")
    public String showEditProductForm(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id).orElseThrow();
        model.addAttribute("product", product);
        model.addAttribute("taxes", taxRateRepository.findAll());
        return "admin/product-form";
    }

    @PostMapping("/save")
    public String saveProduct(@Valid @ModelAttribute Product product,
                              BindingResult bindingResult,
                              @RequestParam(required = false) Long taxRateId,
                              @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
                              Principal principal,
                              RedirectAttributes ra,
                              Model model) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("taxes", taxRateRepository.findAll());
            return "admin/product-form";
        }

        try {
            if (taxRateId != null) {
                TaxRate tr = taxRateRepository.findById(taxRateId)
                        .orElseThrow(() -> new IllegalArgumentException("Neplatná daňová sazba."));
                product.setTaxRate(tr);
            }

            User admin = principal != null ? userRepository.findByEmail(principal.getName()).orElse(null) : null;


            productService.saveProduct(product, imageFile, admin);

            ra.addFlashAttribute("success", "Produkt byl úspěšně uložen.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Nelze uložit data. Zkontrolujte, zda nezadáváte duplicitní údaje.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Při ukládání produktu došlo k systémové chybě: " + e.getMessage());
        }
        return "redirect:/admin/produkty";
    }

    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, Principal principal, RedirectAttributes ra) {
        try {
            User admin = principal != null ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
            productService.deleteProduct(id, admin);
            ra.addFlashAttribute("success", "Produkt byl smazán a přesunut do archivu.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Chyba referenční integrity: Produkt je používán a nelze jej smazat.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Při mazání produktu došlo k neočekávané chybě.");
        }
        return "redirect:/admin/produkty";
    }

    @GetMapping("/{id}/kusovnik")
    public String manageRecipe(@PathVariable Long id, Model model) {
        Product product = productRepository.findById(id).orElseThrow();
        model.addAttribute("product", product);
        model.addAttribute("materials", productService.getAllMaterials());
        return "admin/product-recipe";
    }

    @PostMapping("/{id}/kusovnik/pridat")
    public String addRecipeItem(@PathVariable Long id,
                                @RequestParam Long materialId,
                                @RequestParam Integer quantity,
                                Principal principal,
                                RedirectAttributes ra) {
        try {
            User admin = principal != null ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
            productService.addRecipeItem(id, materialId, quantity, admin);
            ra.addFlashAttribute("success", "Materiál byl úspěšně přidán do kusovníku.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Tento materiál je pravděpodobně v kusovníku již obsažen nebo jsou data neplatná.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Při přidávání materiálu došlo k systémové chybě.");
        }
        return "redirect:/admin/produkty/" + id + "/kusovnik";
    }

    @PostMapping("/{productId}/kusovnik/smazat/{itemId}")
    public String deleteRecipeItem(@PathVariable Long productId,
                                   @PathVariable Long itemId,
                                   Principal principal,
                                   RedirectAttributes ra) {
        try {
            User admin = principal != null ? userRepository.findByEmail(principal.getName()).orElse(null) : null;
            productService.deleteRecipeItem(productId, itemId, admin);
            ra.addFlashAttribute("success", "Položka byla z kusovníku odebrána.");
        } catch (DataIntegrityViolationException e) {
            ra.addFlashAttribute("error", "Položku kusovníku nelze smazat z důvodu vazby na jiná data.");
        } catch (Exception e) {
            ra.addFlashAttribute("error", "Při odebírání položky došlo k systémové chybě.");
        }
        return "redirect:/admin/produkty/" + productId + "/kusovnik";
    }
}