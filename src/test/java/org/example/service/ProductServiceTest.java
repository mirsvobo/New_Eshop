package org.example.service;

import org.example.model.Product;
import org.example.model.RecipeItem;
import org.example.model.User;
import org.example.repository.ProductRepository;
import org.example.repository.RecipeItemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @Mock
    private RecipeItemRepository recipeItemRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private ProductService productService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void saveProduct_WithImage_ShouldSaveImageAndProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        MultipartFile imageFile = mock(MultipartFile.class);
        User user = new User();

        when(imageFile.isEmpty()).thenReturn(false);
        when(fileStorageService.storeFile(imageFile)).thenReturn("image.jpg");

        productService.saveProduct(product, imageFile, user);

        assertEquals("image.jpg", product.getImageUrl());
        verify(productRepository).save(product);
        verify(auditService).log("PRODUKTY", "ÚPRAVA", "Zpracován produkt: Test Product");
    }

    @Test
    void saveProduct_WithoutImage_ShouldSaveProductWithoutChangingImage() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product");
        product.setImageUrl("old_image.jpg");
        MultipartFile imageFile = mock(MultipartFile.class);
        User user = new User();

        when(imageFile.isEmpty()).thenReturn(true);
        Product existingProduct = new Product();
        existingProduct.setImageUrl("old_image.jpg");

        when(productRepository.findById(1L)).thenReturn(Optional.of(existingProduct));

        productService.saveProduct(product, imageFile, user);

        assertEquals("old_image.jpg", product.getImageUrl());
        verify(fileStorageService, never()).storeFile(any());
        verify(productRepository).save(product);
        verify(auditService).log("PRODUKTY", "ÚPRAVA", "Zpracován produkt: Test Product");
    }

    @Test
    void saveProduct_WithDimensions_ShouldSaveProduct() {
        Product product = new Product();
        product.setId(1L);
        product.setName("Test Product with Dimensions");
        product.setWidth(100.0);
        product.setDepth(150.0);
        product.setHeight(200.0);
        product.setVolume(3.0);
        product.setAdditionalDimensions("Boční přesah 10cm");

        MultipartFile imageFile = mock(MultipartFile.class);
        User user = new User();
        when(imageFile.isEmpty()).thenReturn(true);

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        productService.saveProduct(product, imageFile, user);

        verify(productRepository).save(product);
        assertEquals(100.0, product.getWidth());
        assertEquals(150.0, product.getDepth());
        assertEquals(200.0, product.getHeight());
        assertEquals(3.0, product.getVolume());
        assertEquals("Boční přesah 10cm", product.getAdditionalDimensions());
        verify(auditService).log("PRODUKTY", "ÚPRAVA", "Zpracován produkt: Test Product with Dimensions");
    }

    @Test
    void deleteProduct_ShouldDeleteProduct() {
        Long productId = 1L;
        User user = new User();
        Product product = new Product();
        product.setId(productId);
        product.setName("Test Product");

        when(productRepository.findById(productId)).thenReturn(Optional.of(product));

        productService.deleteProduct(productId, user);

        verify(productRepository).deleteById(productId);
        verify(auditService).log("PRODUKTY", "SMAZÁNÍ", "Smazán produkt (Soft Delete): Test Product");
    }

    @Test
    void count_ReturnsTotalProducts() {
        when(productRepository.count()).thenReturn(15L);
        long count = productService.count();
        assertEquals(15L, count);
        verify(productRepository).count();
    }

    @Test
    void addRecipeItem_SuccessfullySavesItem() {
        Product finalProduct = new Product();
        finalProduct.setId(1L);
        finalProduct.setName("Stul");
        Product material = new Product();
        material.setId(2L);
        material.setName("Drevo");

        when(productRepository.findById(1L)).thenReturn(Optional.of(finalProduct));
        when(productRepository.findById(2L)).thenReturn(Optional.of(material));

        User admin = new User();
        productService.addRecipeItem(1L, 2L, 5, admin);

        verify(recipeItemRepository).save(any(RecipeItem.class));
        verify(auditService).log(eq("PRODUKTY"), eq("KUSOVNÍK_PŘIDÁNÍ"), anyString());
    }

    @Test
    void deleteRecipeItem_SuccessfullyDeletesItem() {
        Product finalProduct = new Product();
        finalProduct.setId(1L);
        finalProduct.setName("Stul");

        when(productRepository.findById(1L)).thenReturn(Optional.of(finalProduct));

        User admin = new User();
        productService.deleteRecipeItem(1L, 100L, admin);

        verify(recipeItemRepository).deleteById(100L);
        verify(auditService).log(eq("PRODUKTY"), eq("KUSOVNÍK_SMAZÁNÍ"), anyString());
    }

    @Test
    void getAllMaterials_ReturnsOnlyMaterials() {
        when(productRepository.findByTypeAndIsDeletedFalse(Product.ProductType.MATERIAL)).thenReturn(java.util.Collections.emptyList());

        java.util.List<Product> materials = productService.getAllMaterials();

        assertNotNull(materials);
        verify(productRepository).findByTypeAndIsDeletedFalse(Product.ProductType.MATERIAL);
    }
}