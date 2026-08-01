package org.example.model;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
class ProductImageLayerTest {
    @Test
    void defaultLazure_UsesBaseProductPhotoWithoutImageUrl() {
        ProductImageLayer layer = ProductImageLayer.builder()
                .type(LayerType.LAZURE)
                .optionName("Afromorsia")
                .imageUrl(null)
                .build();
        assertTrue(layer.isDefaultOption());
        assertNull(layer.getDisplayImageUrl());
    }
    @Test
    void defaultRoofColor_UsesBaseProductPhotoWithoutImageUrl() {
        ProductImageLayer layer = ProductImageLayer.builder()
                .type(LayerType.ROOF_COLOR)
                .optionName("Antracit")
                .imageUrl(null)
                .build();
        assertTrue(layer.isDefaultOption());
        assertNull(layer.getDisplayImageUrl());
    }
    @Test
    void nonDefaultOption_ExposesPublicImageUrl() {
        ProductImageLayer layer = ProductImageLayer.builder()
                .type(LayerType.LAZURE)
                .optionName("Kaštan")
                .imageUrl("product-layers/kastan.webp")
                .build();
        assertEquals("/images/product-layers/kastan.webp", layer.getDisplayImageUrl());
    }
}
