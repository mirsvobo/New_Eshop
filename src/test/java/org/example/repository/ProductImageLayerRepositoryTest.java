package org.example.repository;
import org.example.model.LayerType;
import org.example.model.Product;
import org.example.model.ProductImageLayer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.dao.DataIntegrityViolationException;
import java.math.BigDecimal;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class ProductImageLayerRepositoryTest {
    @Autowired
    private ProductImageLayerRepository layerRepository;
    @Autowired
    private ProductRepository productRepository;
    @Test
    void findActiveLayersByType_ReturnsOnlyActiveRowsInConfiguredOrder() {
        Product product = productRepository.save(product("Martin"));
        layerRepository.save(layer(product, LayerType.LAZURE, "Kaštan", 20, true));
        layerRepository.save(layer(product, LayerType.LAZURE, "Afromorsia", -1000, true));
        layerRepository.save(layer(product, LayerType.LAZURE, "Skrytá", 5, false));
        layerRepository.save(layer(product, LayerType.ROOF_COLOR, "Antracit", -1000, true));
        List<ProductImageLayer> result =
                layerRepository.findAllByProductIdAndTypeAndActiveTrueOrderBySortOrderAscOptionNameAsc(
                        product.getId(),
                        LayerType.LAZURE
                );
        assertEquals(2, result.size());
        assertEquals("Afromorsia", result.get(0).getOptionName());
        assertEquals("Kaštan", result.get(1).getOptionName());
    }
    @Test
    void findByIdAndProductId_DoesNotReturnLayerOwnedByAnotherProduct() {
        Product owner = productRepository.save(product("Martin"));
        Product otherProduct = productRepository.save(product("František"));
        ProductImageLayer saved = layerRepository.saveAndFlush(
                layer(owner, LayerType.ROOF_COLOR, "Antracit", -1000, true)
        );
        assertTrue(layerRepository.findByIdAndProductId(saved.getId(), owner.getId()).isPresent());
        assertTrue(layerRepository.findByIdAndProductId(saved.getId(), otherProduct.getId()).isEmpty());
    }
    @Test
    void saveDuplicateProductTypeAndOptionName_ViolatesUniqueConstraint() {
        Product product = productRepository.save(product("Martin"));
        layerRepository.saveAndFlush(layer(product, LayerType.LAZURE, "Kaštan", 0, true));
        assertThrows(
                DataIntegrityViolationException.class,
                () -> layerRepository.saveAndFlush(
                        layer(product, LayerType.LAZURE, "Kaštan", 1, false)
                )
        );
    }
    @Test
    void saveDefaultOptions_AllowsNullImageUrlAndKeepsThemFirst() {
        Product product = productRepository.save(product("Martin"));
        ProductImageLayer defaultLazure = layerRepository.saveAndFlush(
                layer(product, LayerType.LAZURE, "Afromorsia", -1000, true)
        );
        ProductImageLayer defaultRoofColor = layerRepository.saveAndFlush(
                layer(product, LayerType.ROOF_COLOR, "Antracit", -1000, true)
        );
        assertNull(defaultLazure.getImageUrl());
        assertNull(defaultRoofColor.getImageUrl());
        assertTrue(defaultLazure.isDefaultOption());
        assertTrue(defaultRoofColor.isDefaultOption());
    }
    @Test
    void saveNonDefaultOptionWithoutImageUrl_ViolatesCheckConstraint() {
        Product product = productRepository.save(product("Martin"));
        ProductImageLayer invalidLayer = ProductImageLayer.builder()
                .product(product)
                .type(LayerType.LAZURE)
                .optionName("Kaštan")
                .imageUrl(null)
                .sortOrder(0)
                .active(true)
                .build();
        assertThrows(
                DataIntegrityViolationException.class,
                () -> layerRepository.saveAndFlush(invalidLayer)
        );
    }
    @Test
    void saveInactiveDefaultOption_ViolatesCheckConstraint() {
        Product product = productRepository.save(product("Martin"));
        assertThrows(
                DataIntegrityViolationException.class,
                () -> layerRepository.saveAndFlush(
                        layer(product, LayerType.LAZURE, "Afromorsia", -1000, false)
                )
        );
    }
    @Test
    void saveDefaultOptionWithImageUrl_ViolatesCheckConstraint() {
        Product product = productRepository.save(product("Martin"));
        ProductImageLayer invalidDefault = layer(
                product,
                LayerType.ROOF_COLOR,
                "Antracit",
                -1000,
                true
        );
        invalidDefault.setImageUrl("product-layers/antracit.webp");
        assertThrows(
                DataIntegrityViolationException.class,
                () -> layerRepository.saveAndFlush(invalidDefault)
        );
    }
    private Product product(String name) {
        return Product.builder()
                .name(name)
                .price(new BigDecimal("1000"))
                .stockQuantity(1)
                .type(Product.ProductType.PRODUCT)
                .unit("ks")
                .active(true)
                .build();
    }
    private ProductImageLayer layer(
            Product product,
            LayerType type,
            String optionName,
            int sortOrder,
            boolean active
    ) {
        return ProductImageLayer.builder()
                .product(product)
                .type(type)
                .optionName(optionName)
                .imageUrl(type.isDefaultOption(optionName)
                        ? null
                        : "product-layers/" + optionName + ".webp")
                .sortOrder(sortOrder)
                .active(active)
                .build();
    }
}
