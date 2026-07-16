package org.example.service;

import org.example.model.Product;
import org.example.model.RecipeItem;
import org.example.model.User;
import org.example.repository.ProductRepository;
import org.example.repository.RecipeItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final RecipeItemRepository recipeItemRepository;
    private final FileStorageService fileStorageService;
    private final AuditService auditService;

    private static final String MODULE_NAME = "PRODUKTY";

    @Cacheable(value = "products", key = "#root.methodName")
    public long count() {
        return productRepository.count();
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public void saveProduct(Product product, MultipartFile imageFile, User admin) {
        String action = product.getId() == null ? "VYTVOŘENÍ" : "ÚPRAVA";

        if (imageFile != null && !imageFile.isEmpty()) {
            String fileName = fileStorageService.storeFile(imageFile);
            product.setImageUrl(fileName);
        } else if (product.getId() != null) {
            Product existingProduct = productRepository.findById(product.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Produkt nenalezen"));
            product.setImageUrl(existingProduct.getImageUrl());
        }

        productRepository.save(product);
        auditService.log(MODULE_NAME, action, "Zpracován produkt: " + product.getName());
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public void deleteProduct(Long id, User admin) {
        Product product = productRepository.findById(id).orElseThrow();
        productRepository.deleteById(id);
        auditService.log(MODULE_NAME, "SMAZÁNÍ", "Smazán produkt (Soft Delete): " + product.getName());
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public void addRecipeItem(Long productId, Long materialId, Integer quantity, User admin) {
        Product product = productRepository.findById(productId).orElseThrow();
        Product material = productRepository.findById(materialId).orElseThrow();

        RecipeItem recipeItem = RecipeItem.builder()
                .product(product)
                .material(material)
                .quantity(quantity)
                .build();

        recipeItemRepository.save(recipeItem);
        auditService.log(MODULE_NAME, "KUSOVNÍK_PŘIDÁNÍ",
                "Do produktu '" + product.getName() + "' přidán materiál: " + material.getName());
    }

    @CacheEvict(value = "products", allEntries = true)
    @Transactional
    public void deleteRecipeItem(Long productId, Long itemId, User admin) {
        Product product = productRepository.findById(productId).orElseThrow();
        recipeItemRepository.deleteById(itemId);
        auditService.log(MODULE_NAME, "KUSOVNÍK_SMAZÁNÍ",
                "Z produktu '" + product.getName() + "' odstraněn materiál.");
    }

    @Cacheable(value = "products", key = "#root.methodName")
    @Transactional(readOnly = true)
    public List<Product> getAllMaterials() {
        return productRepository.findByTypeAndIsDeletedFalse(Product.ProductType.MATERIAL);
    }
}