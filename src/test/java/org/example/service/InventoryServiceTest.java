package org.example.service;

import org.example.model.Product;
import org.example.model.RecipeItem;
import org.example.model.StockMovement;
import org.example.model.User;
import org.example.repository.ProductRepository;
import org.example.repository.RecipeItemRepository;
import org.example.repository.StockMovementRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InventoryServiceTest {

    @Mock
    private StockMovementRepository stockMovementRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private RecipeItemRepository recipeItemRepository;
    @Mock
    private AuditService auditService;

    @InjectMocks
    private InventoryService inventoryService;

    private Product testProduct;
    private Product testMaterial;
    private RecipeItem testRecipeItem;
    private User testUser;

    @BeforeEach
    void setUp() {
        testProduct = Product.builder()
                .id(1L)
                .name("Káva Espresso")
                .stockQuantity(100)
                .type(Product.ProductType.PRODUCT)
                .unit("ks")
                .build();

        testMaterial = Product.builder()
                .id(2L)
                .name("Kávová zrna")
                .stockQuantity(500)
                .type(Product.ProductType.MATERIAL)
                .unit("g")
                .build();

        testRecipeItem = RecipeItem.builder()
                .id(1L)
                .product(testProduct)
                .material(testMaterial)
                .quantity(10)
                .build();

        testUser = User.builder()
                .id(1L)
                .email("admin@test.cz")
                .role(User.Role.ROLE_ADMIN)
                .build();
    }

    @Test
    void recordMovement_SuccessfullySavesMovement_AndDecrementsStock_OnSale() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        inventoryService.recordMovement(1L, 20, StockMovement.MovementType.SALE, "Prodej 20 ks", testUser);

        assertEquals(80, testProduct.getStockQuantity());
        verify(productRepository, times(1)).save(testProduct);
        verify(stockMovementRepository, times(1)).save(any(StockMovement.class));
    }

    // --- ZMĚNĚNÝ TEST: Testuje manuální výdej (ISSUE), který do mínusu jít nesmí ---
    @Test
    void recordMovement_InsufficientStock_ThrowsException_OnIssue() {
        testProduct.setStockQuantity(10);
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            inventoryService.recordMovement(1L, 20, StockMovement.MovementType.ISSUE, "Výdej přesahující sklad", testUser);
        });

        assertEquals("Na skladě není dostatek položky: Káva Espresso", exception.getMessage());
        verify(stockMovementRepository, never()).save(any());
    }

    // --- NOVÝ TEST: E-shop prodej (SALE) musí dovolit jít do mínusu (výroba na zakázku) ---
    @Test
    void recordMovement_SaleAllowsNegativeStock_ForBackorders() {
        testProduct.setStockQuantity(5); // Skladem jen 5
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        // Zákazník objedná 10
        inventoryService.recordMovement(1L, 10, StockMovement.MovementType.SALE, "Prodej e-shop (na zakázku)", testUser);

        // Očekáváme záporný sklad (-5)
        assertEquals(-5, testProduct.getStockQuantity());
        verify(productRepository, times(1)).save(testProduct);
        verify(stockMovementRepository, times(1)).save(any(StockMovement.class));
    }

    @Test
    void recordMovement_ManualReceipt_DoesNotAffectRecipeMaterials() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        inventoryService.recordMovement(1L, 50, StockMovement.MovementType.RECEIPT, "Manuální naskladnění", testUser);

        assertEquals(150, testProduct.getStockQuantity());
        assertEquals(500, testMaterial.getStockQuantity());

        verify(productRepository).save(testProduct);
        verify(recipeItemRepository, never()).findByProduct(any());
    }

    @Test
    void produceProduct_SuccessfullyIncrementsProduct_AndDeductsMaterials_WithRecipe() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(recipeItemRepository.findByProduct(testProduct)).thenReturn(List.of(testRecipeItem));
        when(stockMovementRepository.save(any(StockMovement.class))).thenAnswer(i -> {
            StockMovement sm = i.getArgument(0);
            sm.setId(100L);
            return sm;
        });

        inventoryService.produceProduct(1L, 20, testUser);

        assertEquals(120, testProduct.getStockQuantity());
        assertEquals(300, testMaterial.getStockQuantity());

        verify(productRepository).save(testProduct);
        verify(productRepository).saveAll(anyList());
        verify(stockMovementRepository, atLeastOnce()).save(any(StockMovement.class));
        verify(stockMovementRepository).saveAll(anyList());
    }

    @Test
    void produceProduct_SuccessfullyIncrementsProduct_WhenNoRecipeExists() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(recipeItemRepository.findByProduct(testProduct)).thenReturn(Collections.emptyList());

        inventoryService.produceProduct(1L, 15, testUser);

        assertEquals(115, testProduct.getStockQuantity());

        verify(productRepository).save(testProduct);
        verify(stockMovementRepository, times(1)).save(any(StockMovement.class));
        verify(productRepository, never()).saveAll(anyList());
    }

    @Test
    void produceProduct_ThrowsException_WhenProductIsMaterial() {
        when(productRepository.findById(2L)).thenReturn(Optional.of(testMaterial));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            inventoryService.produceProduct(2L, 10, testUser);
        });

        assertEquals("Do výroby lze zadat pouze produkty typu PRODUCT.", exception.getMessage());
        verify(stockMovementRepository, never()).save(any());
    }
}