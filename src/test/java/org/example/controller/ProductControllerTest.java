package org.example.controller;
import org.example.model.LayerType;
import org.example.model.Product;
import org.example.model.ProductImageLayer;
import org.example.model.TaxMode;
import org.example.model.TaxRate;
import org.example.repository.ProductRepository;
import org.example.service.Cart;
import org.example.service.ProductImageLayerService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Sort;
import org.springframework.test.web.servlet.MockMvc;
import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(ProductController.class)
class ProductControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ProductRepository productRepository;
    @MockitoBean
    private ProductImageLayerService productImageLayerService;
    @MockitoBean(name = "cart")
    private Cart cart;
    @BeforeEach
    void setUp() {
        when(cart.getTaxMode()).thenReturn(TaxMode.STANDARD);
        when(cart.getTotalItems()).thenReturn(0);
    }
    @Test
    @WithMockUser
    void shouldReturnActiveProductsSortedByPrice() throws Exception {
        given(productRepository.findByActiveTrueAndType(eq(Product.ProductType.PRODUCT), any(Sort.class)))
                .willReturn(Collections.emptyList());
        mockMvc.perform(get("/produkty").param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(view().name("produkty"))
                .andExpect(model().attribute("currentSort", "price_asc"));
    }
    @Test
    @WithMockUser
    void shouldShowProductDetailWhenActiveAndTypeIsProduct() throws Exception {
        TaxRate standardTax = TaxRate.builder().rate(new BigDecimal("21.0")).build();
        Product product = Product.builder()
                .id(1L)
                .name("Pracovní Stůl")
                .active(true)
                .type(Product.ProductType.PRODUCT)
                .price(new BigDecimal("1000.00"))
                .stockQuantity(10.0)
                .unit("ks")
                .taxRate(standardTax)
                .build();
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(productImageLayerService.getActiveLayersForProduct(1L)).willReturn(Collections.emptyList());
        mockMvc.perform(get("/produkty/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("produkt-detail"))
                .andExpect(model().attribute("product", product))
                .andExpect(model().attributeExists("lazureLayers", "roofColorLayers"));
    }
    @Test
    @WithMockUser
    void shouldRenderBackendDefaultsWithoutFilesAndOtherOptionsAsImageLayers() throws Exception {
        TaxRate standardTax = TaxRate.builder().rate(new BigDecimal("21.0")).build();
        Product product = Product.builder()
                .id(1L)
                .name("Dřevník František")
                .imageUrl("products/frantisek.webp")
                .active(true)
                .type(Product.ProductType.PRODUCT)
                .price(new BigDecimal("25000.00"))
                .stockQuantity(3.0)
                .unit("ks")
                .taxRate(standardTax)
                .build();
        ProductImageLayer defaultLazure = ProductImageLayer.builder()
                .id(11L)
                .product(product)
                .type(LayerType.LAZURE)
                .optionName("Afromorsia")
                .imageUrl(null)
                .sortOrder(-1000)
                .active(true)
                .build();
        ProductImageLayer chestnutLazure = ProductImageLayer.builder()
                .id(12L)
                .product(product)
                .type(LayerType.LAZURE)
                .optionName("Kaštan")
                .imageUrl("product-layers/kastan.webp")
                .sortOrder(10)
                .active(true)
                .build();
        ProductImageLayer defaultRoofColor = ProductImageLayer.builder()
                .id(13L)
                .product(product)
                .type(LayerType.ROOF_COLOR)
                .optionName("Antracit")
                .imageUrl(null)
                .sortOrder(-1000)
                .active(true)
                .build();
        ProductImageLayer redRoofColor = ProductImageLayer.builder()
                .id(14L)
                .product(product)
                .type(LayerType.ROOF_COLOR)
                .optionName("Červená")
                .imageUrl("product-layers/cervena.webp")
                .sortOrder(10)
                .active(true)
                .build();
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(productImageLayerService.getActiveLayersForProduct(1L))
                .willReturn(List.of(defaultLazure, chestnutLazure, defaultRoofColor, redRoofColor));
        String html = mockMvc.perform(get("/produkty/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("produkt-detail"))
                .andExpect(model().attribute("lazureLayers", List.of(defaultLazure, chestnutLazure)))
                .andExpect(model().attribute("roofColorLayers", List.of(defaultRoofColor, redRoofColor)))
                .andExpect(content().string(containsString("id=\"productImageLayerContainer\"")))
                .andExpect(content().string(containsString("aspect-square")))
                .andExpect(content().string(containsString("id=\"productBaseImage\"")))
                .andExpect(content().string(containsString("src=\"/images/products/frantisek.webp\"")))
                .andExpect(content().string(containsString("id=\"productLazureLayer\"")))
                .andExpect(content().string(containsString("id=\"productRoofColorLayer\"")))
                .andExpect(content().string(containsString("data-image-url=\"/images/product-layers/kastan.webp\"")))
                .andExpect(content().string(containsString("data-image-url=\"/images/product-layers/cervena.webp\"")))
                .andExpect(content().string(containsString("id=\"lazureSelect\"")))
                .andExpect(content().string(containsString("name=\"selectedLazure\"")))
                .andExpect(content().string(containsString("Afromorsia")))
                .andExpect(content().string(containsString("id=\"roofColorSelect\"")))
                .andExpect(content().string(containsString("name=\"selectedRoofColor\"")))
                .andExpect(content().string(containsString("Antracit")))
                .andExpect(content().string(containsString("aria-live=\"polite\"")))
                .andExpect(content().string(containsString("updateImageLayer")))
                .andExpect(content().string(containsString("usesBasePhoto")))
                .andExpect(content().string(containsString("addEventListener('change'")))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertAppearsBefore(
                html,
                "id=\"productBaseImage\"",
                "id=\"productLazureLayer\"",
                "Základní obrázek musí být v HTML před vrstvou lazury."
        );
        assertAppearsBefore(
                html,
                "id=\"productLazureLayer\"",
                "id=\"productRoofColorLayer\"",
                "Vrstva lazury musí být v HTML před vrstvou střechy."
        );
        String lazureLayerTag = findStartTag(html, "id=\"productLazureLayer\"");
        String roofColorLayerTag = findStartTag(html, "id=\"productRoofColorLayer\"");
        String defaultLazureOptionTag = findStartTag(html, "value=\"Afromorsia\"");
        String defaultRoofColorOptionTag = findStartTag(html, "value=\"Antracit\"");
        assertFalse(
                lazureLayerTag.contains("src="),
                "Výchozí Afromorsia nesmí mít překryvný obrázek."
        );
        assertFalse(
                roofColorLayerTag.contains("src="),
                "Výchozí Antracit nesmí mít překryvný obrázek."
        );
        assertTrue(lazureLayerTag.contains("hidden"));
        assertTrue(roofColorLayerTag.contains("hidden"));
        assertTrue(defaultLazureOptionTag.contains("selected"));
        assertTrue(defaultRoofColorOptionTag.contains("selected"));
        assertFalse(defaultLazureOptionTag.contains("data-image-url"));
        assertFalse(defaultRoofColorOptionTag.contains("data-image-url"));
        assertEquals(
                3,
                countOccurrences(html, "width=\"800\""),
                "Všechny tři obrázky musí deklarovat shodnou šířku 800 px."
        );
        assertEquals(
                3,
                countOccurrences(html, "height=\"800\""),
                "Všechny tři obrázky musí deklarovat shodnou výšku 800 px."
        );
        assertEquals(
                3,
                countOccurrences(html, "h-full w-full select-none object-contain pointer-events-none"),
                "Všechny tři vrstvy musí používat shodné rozměrové CSS vlastnosti."
        );
    }
    @Test
    @WithMockUser
    void shouldRedirectToProductListWhenProductIsMaterial() throws Exception {
        Product material = Product.builder()
                .id(1L)
                .active(true)
                .type(Product.ProductType.MATERIAL)
                .build();
        given(productRepository.findById(1L)).willReturn(Optional.of(material));
        mockMvc.perform(get("/produkty/1"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/produkty"));
    }
    @Test
    @WithMockUser
    void listProducts_WithSorting_ReturnsSortedProducts() throws Exception {
        given(productRepository.findByActiveTrueAndType(eq(Product.ProductType.PRODUCT), any(Sort.class)))
                .willReturn(java.util.Collections.emptyList());
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/produkty").param("sort", "price_desc"))
                .andExpect(status().isOk())
                .andExpect(view().name("produkty"))
                .andExpect(model().attribute("currentSort", "price_desc"));
    }
    private int countOccurrences(String text, String searchedValue) {
        int count = 0;
        int currentIndex = 0;
        while ((currentIndex = text.indexOf(searchedValue, currentIndex)) != -1) {
            count++;
            currentIndex += searchedValue.length();
        }
        return count;
    }
    private String findStartTag(String html, String marker) {
        int markerIndex = html.indexOf(marker);
        assertTrue(markerIndex >= 0, "V HTML chybí hodnota: " + marker);
        int tagStart = html.lastIndexOf('<', markerIndex);
        int tagEnd = html.indexOf('>', markerIndex);
        assertTrue(tagStart >= 0, "Nelze najít začátek HTML tagu pro: " + marker);
        assertTrue(tagEnd > markerIndex, "Nelze najít konec HTML tagu pro: " + marker);
        return html.substring(tagStart, tagEnd + 1);
    }
    private void assertAppearsBefore(
            String html,
            String firstValue,
            String secondValue,
            String message
    ) {
        int firstIndex = html.indexOf(firstValue);
        int secondIndex = html.indexOf(secondValue);
        assertTrue(firstIndex >= 0, "V HTML chybí hodnota: " + firstValue);
        assertTrue(secondIndex >= 0, "V HTML chybí hodnota: " + secondValue);
        assertTrue(firstIndex < secondIndex, message);
    }
}
