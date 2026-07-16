package org.example.service;

import org.example.dto.CartItemDto;
import org.example.dto.CheckoutFormDataDto;
import org.example.model.*;
import org.example.repository.CouponRepository;
import org.example.repository.OrderRepository;
import org.example.repository.OrderStatusRepository;
import org.example.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final OrderStatusRepository orderStatusRepository;
    private final InventoryService inventoryService;
    private final Cart cart;
    private final LocalInvoiceService invoiceService;
    private final CouponRepository couponRepository;
    private final AuditService auditService;

    private static final String MODULE_NAME = "OBJEDNÁVKY";

    @Value("${erp.shipping.default-cost:150.00}")
    private BigDecimal defaultShippingCost;

    @Transactional
    public Order processCheckout(User customer, CheckoutFormDataDto formData, String couponCode) {
        Order order = initializeOrderInfo(customer, formData);
        buildOrderAddresses(order, formData);

        order.setShippingCost(defaultShippingCost);
        BigDecimal itemsTotal = processCartItemsAndInventory(order, customer);

        BigDecimal discount = applyCouponIfValid(order, itemsTotal, couponCode);
        order.setDiscountAmount(discount);

        BigDecimal finalTotal = itemsTotal.subtract(discount).add(order.getShippingCost());
        if (finalTotal.compareTo(BigDecimal.ZERO) < 0) {
            finalTotal = BigDecimal.ZERO;
        }
        order.setTotalAmount(finalTotal);

        initializeOrderStatus(order, customer);

        Order savedOrder = orderRepository.save(order);
        invoiceService.generateHtmlInvoice(savedOrder);
        cart.clear();

        auditService.log(MODULE_NAME, "Nová objednávka",
                "Vytvořena objednávka č. " + savedOrder.getOrderNumber() + " v hodnotě " + savedOrder.getTotalAmount() + " Kč.");

        return savedOrder;
    }

    private Order initializeOrderInfo(User customer, CheckoutFormDataDto formData) {
        Order order = new Order();
        order.setCustomer(customer);
        order.setOrderNumber(generateOrderNumber());
        order.setGuestFirstName(formData.getFirstName());
        order.setGuestLastName(formData.getLastName());
        order.setGuestEmail(formData.getEmail());
        order.setGuestPhone(formData.getPhone());
        order.setIco(formData.getIco());
        order.setDic(formData.getDic());
        return order;
    }

    private void buildOrderAddresses(Order order, CheckoutFormDataDto formData) {
        String bName = (formData.getCompanyName() != null && !formData.getCompanyName().isBlank())
                ? formData.getCompanyName()
                : (formData.getFirstName() + " " + formData.getLastName());

        String bAddr = String.format("%s, %s, %s %s", bName, formData.getBillingStreet(), formData.getBillingZipCode(), formData.getBillingCity());
        order.setBillingAddress(bAddr);

        if (formData.isShipToDifferentAddress()) {
            order.setDeliveryAddress(String.format("%s, %s %s", formData.getShippingStreet(), formData.getShippingZipCode(), formData.getShippingCity()));
        } else {
            order.setDeliveryAddress(bAddr);
        }
    }

    private BigDecimal processCartItemsAndInventory(Order order, User customer) {
        BigDecimal total = BigDecimal.ZERO;
        List<Long> productIds = cart.getItems().stream().map(CartItemDto::getProductId).toList();

        Map<Long, Product> productCache = productRepository.findAllById(productIds).stream()
                .collect(Collectors.toMap(Product::getId, p -> p));

        for (CartItemDto item : cart.getItems()) {
            Product product = productCache.get(item.getProductId());
            if (product == null) {
                throw new IllegalStateException("Produkt nebyl nalezen: " + item.getProductId());
            }

            inventoryService.recordMovement(
                    product.getId(),
                    item.getQuantity(),
                    StockMovement.MovementType.SALE,
                    "Prodej - Objednávka: " + order.getOrderNumber(),
                    customer
            );

            OrderItem oi = OrderItem.builder()
                    .order(order)
                    .product(product)
                    .quantity(item.getQuantity())
                    .unitPrice(item.getPrice())
                    .build();

            order.getItems().add(oi);
            total = total.add(oi.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        return total;
    }

    private void initializeOrderStatus(Order order, User customer) {
        OrderStatus defaultStatus = orderStatusRepository.findByName("Nová")
                .orElseThrow(() -> new IllegalStateException("Výchozí stav 'Nová' nenalezen."));

        order.setStatus(defaultStatus);

        OrderStatusHistory initialHistory = OrderStatusHistory.builder()
                .order(order)
                .status(defaultStatus)
                .note("Objednávka přijata.")
                .createdAt(LocalDateTime.now())
                .changedBy(customer)
                .build();

        order.getStatusHistory().add(initialHistory);
    }

    private String generateOrderNumber() {
        int currentYear = LocalDate.now().getYear();
        long nextSequence = orderRepository.count() + 1;
        return String.format("ORD-%d-%05d", currentYear, nextSequence);
    }

    private BigDecimal applyCouponIfValid(Order order, BigDecimal itemsTotal, String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return BigDecimal.ZERO;
        }

        Coupon cartCoupon = cart.getAppliedCoupon();
        if (cartCoupon != null && cartCoupon.getCode().equalsIgnoreCase(couponCode.trim())) {
            order.setAppliedCoupon(cartCoupon);
            return cart.getDiscountAmount();
        }


        return couponRepository.findByCodeAndActiveTrue(couponCode)
                .map(coupon -> {
                    order.setAppliedCoupon(coupon);
                    if (coupon.getType() == DiscountType.PERCENTAGE) {
                        BigDecimal percentage = coupon.getDiscountValue().divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
                        return itemsTotal.multiply(percentage).setScale(2, RoundingMode.HALF_UP);
                    } else {
                        return coupon.getDiscountValue().compareTo(itemsTotal) > 0 ? itemsTotal : coupon.getDiscountValue();
                    }
                })
                .orElse(BigDecimal.ZERO);
    }
}