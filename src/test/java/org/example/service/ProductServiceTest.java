package org.example.service;

import org.example.model.Product;
import org.example.model.User;
import org.example.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.multipart.MultipartFile;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

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
}