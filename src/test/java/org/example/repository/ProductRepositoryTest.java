package org.example.repository;

import org.example.model.Product;
import org.example.model.TaxRate;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.math.BigDecimal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TaxRateRepository taxRateRepository;

    @Test
    void saveProduct_WithPhysicalDimensions_ShouldPersistCorrectly() {
        TaxRate tax = TaxRate.builder()
                .name("DPH 21%")
                .rate(new BigDecimal("21"))
                .defaultRate(true)
                .build();
        taxRateRepository.save(tax);

        Product product = Product.builder()
                .name("Zahradní domek s rozměry")
                .price(new BigDecimal("15000"))
                .taxRate(tax)
                .stockQuantity(5)
                .type(Product.ProductType.PRODUCT)
                .unit("ks")
                .width(250.0)
                .depth(300.0)
                .height(220.5)
                .volume(16.5)
                .additionalDimensions("Přesah střechy: 15 cm boční")
                .active(true)
                .build();

        Product saved = productRepository.save(product);

        Optional<Product> retrieved = productRepository.findById(saved.getId());
        assertTrue(retrieved.isPresent());
        assertEquals(250.0, retrieved.get().getWidth());
        assertEquals(300.0, retrieved.get().getDepth());
        assertEquals(220.5, retrieved.get().getHeight());
        assertEquals(16.5, retrieved.get().getVolume());
        assertEquals("Přesah střechy: 15 cm boční", retrieved.get().getAdditionalDimensions());
    }
}