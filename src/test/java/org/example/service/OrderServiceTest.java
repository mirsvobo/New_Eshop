package org.example.service;

import org.example.dto.CartItemDto;
import org.example.dto.CheckoutFormDataDto;
import org.example.model.Order;
import org.example.model.OrderStatus;
import org.example.model.Product;
import org.example.model.StockMovement;
import org.example.model.TaxMode;
import org.example.model.User;
import org.example.repository.CouponRepository;
import org.example.repository.OrderRepository;
import org.example.repository.OrderStatusRepository;
import org.example.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private OrderStatusRepository orderStatusRepository;
    @Mock
    private InventoryService inventoryService;
    @Mock
    private LocalInvoiceService invoiceService;
    @Mock
    private CouponRepository couponRepository;
    @Mock
    private Cart cart;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private OrderService orderService;

    private CheckoutFormDataDto formData;
    private Product testProduct;
    private OrderStatus novaStatus;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(orderService, "defaultShippingCost", new BigDecimal("150.00"));

        formData = new CheckoutFormDataDto();
        formData.setFirstName("Jan");
        formData.setLastName("Novák");
        formData.setEmail("jan@test.cz");
        formData.setBillingStreet("Testovací 1");
        formData.setBillingZipCode("11100");
        formData.setBillingCity("Praha");

        testProduct = Product.builder().id(1L).name("Test Product").build();
        novaStatus = OrderStatus.builder().id(1L).name("Nová").build();
    }

    @Test
    void processCheckout_WithGuestUser() {
        when(orderStatusRepository.findByName("Nová")).thenReturn(Optional.of(novaStatus));

        CartItemDto item = CartItemDto.builder()
                .productId(1L)
                .productName("Test Product")
                .quantity(2)
                .price(new BigDecimal("500.00"))
                .basePrice(BigDecimal.ZERO)
                .originalPrice(new BigDecimal("1000.00"))
                .taxRateValue(new BigDecimal("21.00"))
                .stockQuantity(10.0)
                .build();

        when(cart.getItems()).thenReturn(Arrays.asList(item));
        when(productRepository.findAllById(any())).thenReturn(Arrays.asList(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(100L);
            return order;
        });

        Order result = orderService.processCheckout(null, formData, null);

        assertNotNull(result);
        assertNull(result.getCustomer());
        verify(inventoryService, times(1)).recordMovement(eq(1L), eq(2.0), eq(StockMovement.MovementType.SALE), anyString(), isNull());
        verify(orderRepository, times(1)).save(any(Order.class));
        verify(invoiceService, times(1)).generateHtmlInvoice(any(Order.class));
        verify(cart, times(1)).clear();
    }

    @Test
    void processCheckout_WithRegisteredUser() {
        when(orderStatusRepository.findByName("Nová")).thenReturn(Optional.of(novaStatus));

        User registeredUser = User.builder().id(1L).email("user@test.cz").firstName("Registered").lastName("User").build();
        CartItemDto item = CartItemDto.builder()
                .productId(1L)
                .productName("Test Product")
                .quantity(1)
                .price(new BigDecimal("500.00"))
                .basePrice(BigDecimal.ZERO)
                .originalPrice(new BigDecimal("500.00"))
                .taxRateValue(new BigDecimal("21.00"))
                .stockQuantity(10.0)
                .build();

        when(cart.getItems()).thenReturn(Arrays.asList(item));
        when(productRepository.findAllById(any())).thenReturn(Arrays.asList(testProduct));
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.processCheckout(registeredUser, formData, null);

        assertNotNull(result);
        assertEquals(registeredUser, result.getCustomer());
        verify(inventoryService, times(1)).recordMovement(eq(1L), eq(1.0), eq(StockMovement.MovementType.SALE), anyString(), eq(registeredUser));
    }

    @Test
    void processCheckout_EmptyCart_ReturnsOrderWithShippingCostOnly() {
        when(orderStatusRepository.findByName("Nová")).thenReturn(Optional.of(novaStatus));

        when(cart.getItems()).thenReturn(Collections.emptyList());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.processCheckout(null, formData, null);

        assertNotNull(result);
        assertTrue(result.getItems().isEmpty());
        assertEquals(0, new BigDecimal("150.00").compareTo(result.getTotalAmount()));
    }

    @Test
    void processCheckout_WithReducedTaxMode_SavesOrderAndItemsCorrectly() {
        when(orderStatusRepository.findByName("Nová")).thenReturn(Optional.of(novaStatus));

        CartItemDto item = CartItemDto.builder()
                .productId(1L)
                .productName("Test Product")
                .quantity(1)
                .price(new BigDecimal("112.00"))
                .basePrice(new BigDecimal("100.00"))
                .taxRateValue(new BigDecimal("21.00"))
                .build();

        when(cart.getItems()).thenReturn(Arrays.asList(item));
        when(productRepository.findAllById(any())).thenReturn(Arrays.asList(testProduct));
        formData.setTaxMode(TaxMode.REDUCED);
        formData.setAffidavitSigned(true);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.processCheckout(null, formData, null);

        assertNotNull(result);
        assertEquals(TaxMode.REDUCED, result.getTaxMode(), "Objednávka musí být uložena v režimu REDUCED");
        assertTrue(result.isAffidavitSigned(), "Objednávka musí mít znak podepsaného čestného prohlášení");
        assertFalse(result.getItems().isEmpty());
        assertEquals(0, new BigDecimal("12.00").compareTo(result.getItems().get(0).getActualTaxRate()),
                "Položka objednávky si musí pro účetnictví fixně uložit 12 % DPH bez ohledu na produkt v DB");
    }

    @Test
    void processCheckout_WithStandardTaxMode_SavesOrderAndItemsCorrectly() {
        when(orderStatusRepository.findByName("Nová")).thenReturn(Optional.of(novaStatus));

        CartItemDto item = CartItemDto.builder()
                .productId(1L)
                .productName("Test Product")
                .quantity(1)
                .price(new BigDecimal("121.00"))
                .basePrice(new BigDecimal("100.00"))
                .taxRateValue(new BigDecimal("21.00"))
                .build();

        when(cart.getItems()).thenReturn(Arrays.asList(item));
        when(productRepository.findAllById(any())).thenReturn(Arrays.asList(testProduct));
        formData.setTaxMode(TaxMode.STANDARD);
        formData.setAffidavitSigned(false);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.processCheckout(null, formData, null);

        assertNotNull(result);
        assertEquals(TaxMode.STANDARD, result.getTaxMode(), "Objednávka musí být uložena v režimu STANDARD");
        assertFalse(result.isAffidavitSigned(), "Objednávka nesmí mít podepsané čestné prohlášení v režimu STANDARD");
        assertFalse(result.getItems().isEmpty());
        assertEquals(0, new BigDecimal("21.00").compareTo(result.getItems().get(0).getActualTaxRate()),
                "Položka objednávky si musí uložit originální 21 % DPH");
    }

    @Test
    void processCheckout_WithProductVariants_SavesOrderItemsCorrectly() {
        when(orderStatusRepository.findByName("Nová")).thenReturn(Optional.of(novaStatus));

        CartItemDto item = CartItemDto.builder()
                .productId(1L)
                .productName("Test Product Variants")
                .quantity(1)
                .price(new BigDecimal("121.00"))
                .basePrice(new BigDecimal("100.00"))
                .taxRateValue(new BigDecimal("21.00"))
                .selectedLazure("Ořech")
                .selectedRoofColor("Černá")
                .selectedDesign("Klasik")
                .build();

        when(cart.getItems()).thenReturn(Arrays.asList(item));
        when(productRepository.findAllById(any())).thenReturn(Arrays.asList(testProduct));
        formData.setTaxMode(TaxMode.STANDARD);
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.processCheckout(null, formData, null);

        assertNotNull(result);
        assertFalse(result.getItems().isEmpty());
        assertEquals("Ořech", result.getItems().get(0).getSelectedLazure(), "Lazura se musí správně překopírovat do OrderItem");
        assertEquals("Černá", result.getItems().get(0).getSelectedRoofColor(), "Barva střechy se musí správně překopírovat do OrderItem");
        assertEquals("Klasik", result.getItems().get(0).getSelectedDesign(), "Design se musí správně překopírovat do OrderItem");
    }
    @Test
    void processCheckout_WithInvalidCoupon_SavesOrderWithoutDiscount() {
        when(orderStatusRepository.findByName("Nová")).thenReturn(Optional.of(novaStatus));

        CartItemDto item = CartItemDto.builder()
                .productId(1L)
                .productName("Test Product")
                .quantity(1)
                .price(new BigDecimal("100.00"))
                .basePrice(new BigDecimal("82.64"))
                .taxRateValue(new BigDecimal("21.00"))
                .build();
        when(cart.getItems()).thenReturn(java.util.Arrays.asList(item));
        when(productRepository.findAllById(any())).thenReturn(java.util.Arrays.asList(testProduct));
        when(cart.getAppliedCoupon()).thenReturn(null);
        when(couponRepository.findByCodeAndActiveTrue("INVALID")).thenReturn(Optional.empty());
        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.processCheckout(null, formData, "INVALID");

        assertNotNull(result);
        assertEquals(0, BigDecimal.ZERO.compareTo(result.getDiscountAmount()), "U neplatného kupónu musí být sleva 0.");
        assertNull(result.getAppliedCoupon(), "Při neplatném kupónu nesmí být k objednávce přiřazen žádný kupón.");
    }

    @Test
    void processCheckout_WithValidCartCoupon_AppliesDiscount() {
        when(orderStatusRepository.findByName("Nová")).thenReturn(Optional.of(novaStatus));

        CartItemDto item = CartItemDto.builder()
                .productId(1L)
                .productName("Test Product")
                .quantity(1)
                .price(new BigDecimal("100.00"))
                .basePrice(new BigDecimal("82.64"))
                .taxRateValue(new BigDecimal("21.00"))
                .build();
        when(cart.getItems()).thenReturn(java.util.Arrays.asList(item));
        when(productRepository.findAllById(any())).thenReturn(java.util.Arrays.asList(testProduct));

        org.example.model.Coupon coupon = new org.example.model.Coupon();
        coupon.setCode("VALID10");
        when(cart.getAppliedCoupon()).thenReturn(coupon);
        when(cart.getDiscountAmount()).thenReturn(new BigDecimal("10.00"));

        when(orderRepository.save(any(Order.class))).thenAnswer(i -> i.getArgument(0));

        Order result = orderService.processCheckout(null, formData, "VALID10");

        assertNotNull(result);
        assertEquals(0, new BigDecimal("10.00").compareTo(result.getDiscountAmount()), "Sleva musí odpovídat výpočtu košíku.");
        assertEquals(coupon, result.getAppliedCoupon(), "Objednávka musí mít správně navázaný aplikovaný kupón.");
    }
}