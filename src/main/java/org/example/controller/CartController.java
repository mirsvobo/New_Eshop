package org.example.controller;

import org.example.dto.CartItemDto;
import org.example.model.LayerType;
import org.example.model.Product;
import org.example.model.TaxMode;
import org.example.repository.ProductRepository;
import org.example.service.Cart;
import org.example.service.ProductImageLayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;

@Controller
@RequestMapping("/kosik")
@RequiredArgsConstructor
public class CartController {

    private final Cart cart;
    private final ProductRepository productRepository;
    private final ProductImageLayerService productImageLayerService;

    @GetMapping
    public String viewCart(Model model) {
        model.addAttribute("cart", cart);
        model.addAttribute("cartItems", cart.getItems());
        model.addAttribute("cartTotal", cart.getTotalPrice());
        model.addAttribute("discount", cart.getDiscountAmount());
        model.addAttribute("finalTotal", cart.getFinalPrice());
        return "kosik";
    }

    @PostMapping("/pridat")
    public String addToCart(@RequestParam Long productId,
                            @RequestParam Integer quantity,
                            @RequestParam(required = false) String selectedLazure,
                            @RequestParam(required = false) String selectedRoofColor,
                            RedirectAttributes redirectAttributes) {
        Product product = productRepository.findById(productId).orElse(null);
        if (product == null) {
            redirectAttributes.addFlashAttribute("error", "Produkt nebyl nalezen.");
            return "redirect:/produkty";
        }

        String validatedLazure;
        String validatedRoofColor;
        try {
            validatedLazure = productImageLayerService.validateAndResolveSelection(
                    productId,
                    LayerType.LAZURE,
                    selectedLazure
            );
            validatedRoofColor = productImageLayerService.validateAndResolveSelection(
                    productId,
                    LayerType.ROOF_COLOR,
                    selectedRoofColor
            );
        } catch (IllegalArgumentException exception) {
            redirectAttributes.addFlashAttribute("error", exception.getMessage());
            return "redirect:/produkty/" + productId;
        }

        BigDecimal taxMultiplier = (product.getTaxRate() != null)
                ? product.getTaxRate().getRate().divide(new BigDecimal("100")).add(BigDecimal.ONE)
                : BigDecimal.ONE;
        BigDecimal originalPriceWithTax = product.getPrice().multiply(taxMultiplier);

        cart.addItem(CartItemDto.builder()
                .productId(product.getId())
                .productName(product.getName())
                .imageUrl(product.getDisplayImageUrl())
                .quantity(quantity)
                .price(product.getPriceWithTax())
                .basePrice(product.getActiveBasePrice())
                .originalPrice(originalPriceWithTax)
                .taxRateValue(product.getTaxRate() != null ? product.getTaxRate().getRate() : BigDecimal.ZERO)
                .stockQuantity(product.getStockQuantity())
                .selectedLazure(validatedLazure)
                .selectedRoofColor(validatedRoofColor)
                .build());

        return "redirect:/kosik";
    }

    @PostMapping("/upravit")
    public String updateCartItem(@RequestParam Long productId, @RequestParam Integer quantity) {
        cart.updateQuantity(productId, quantity);
        return "redirect:/kosik";
    }

    @PostMapping("/odstranit")
    public String removeFromCart(@RequestParam Long productId) {
        cart.getItems().removeIf(item -> item.getProductId().equals(productId));
        return "redirect:/kosik";
    }

    @PostMapping("/vycistit")
    public String clearCart() {
        cart.clear();
        return "redirect:/kosik";
    }

    @PostMapping("/rezim")
    public String switchTaxMode(@RequestParam TaxMode taxMode) {
        cart.setTaxMode(taxMode);
        return "redirect:/kosik";
    }
}
