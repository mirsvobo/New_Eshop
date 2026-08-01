package org.example.service;
import org.example.model.LayerType;
import org.example.model.Product;
import org.example.model.ProductImageLayer;
import org.example.repository.ProductImageLayerRepository;
import org.example.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
@ExtendWith(MockitoExtension.class)
class ProductImageLayerServiceTest {
    @Mock
    private ProductImageLayerRepository layerRepository;
    @Mock
    private ProductRepository productRepository;
    @Mock
    private FileStorageService fileStorageService;
    @Mock
    private WebpImageValidator webpImageValidator;
    @InjectMocks
    private ProductImageLayerService service;
    private Product product;
    private MockMultipartFile validImage;
    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Martin")
                .type(Product.ProductType.PRODUCT)
                .build();
        validImage = new MockMultipartFile(
                "layerImageFile",
                "kastan.webp",
                "image/webp",
                new byte[]{1, 2, 3}
        );
    }
    @Test
    void getLayersForProduct_ReturnsOrderedRepositoryResult() {
        ProductImageLayer layer = layer(10L, product, LayerType.LAZURE, "Kaštan", "product-layers/a.webp");
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(layerRepository.findAllByProductIdOrderByTypeAscSortOrderAscOptionNameAsc(1L))
                .thenReturn(List.of(layer));
        List<ProductImageLayer> result = service.getLayersForProduct(1L);
        assertEquals(List.of(layer), result);
    }
    @Test
    void ensureDefaultVariants_CreatesActiveOptionsWithoutImageFiles() {
        when(layerRepository.findFirstByProductIdAndTypeAndOptionNameIgnoreCase(
                1L,
                LayerType.LAZURE,
                "Afromorsia"
        )).thenReturn(Optional.empty());
        when(layerRepository.findFirstByProductIdAndTypeAndOptionNameIgnoreCase(
                1L,
                LayerType.ROOF_COLOR,
                "Antracit"
        )).thenReturn(Optional.empty());
        when(layerRepository.saveAndFlush(any(ProductImageLayer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service.ensureDefaultVariants(product);
        ArgumentCaptor<ProductImageLayer> captor = ArgumentCaptor.forClass(ProductImageLayer.class);
        verify(layerRepository, times(2)).saveAndFlush(captor.capture());
        List<ProductImageLayer> defaults = captor.getAllValues();
        assertEquals(2, defaults.size());
        assertEquals("Afromorsia", defaults.get(0).getOptionName());
        assertEquals(LayerType.LAZURE, defaults.get(0).getType());
        assertNull(defaults.get(0).getImageUrl());
        assertEquals(-1000, defaults.get(0).getSortOrder());
        assertTrue(defaults.get(0).isActive());
        assertEquals("Antracit", defaults.get(1).getOptionName());
        assertEquals(LayerType.ROOF_COLOR, defaults.get(1).getType());
        assertNull(defaults.get(1).getImageUrl());
        assertEquals(-1000, defaults.get(1).getSortOrder());
        assertTrue(defaults.get(1).isActive());
        verifyNoInteractions(webpImageValidator, fileStorageService);
    }
    @Test
    void ensureDefaultVariants_ConvertsExistingDefaultAndRemovesObsoleteFile() {
        ProductImageLayer lazure = layer(
                10L,
                product,
                LayerType.LAZURE,
                "afromorsia",
                "product-layers/old-afromorsia.webp"
        );
        lazure.setActive(false);
        lazure.setSortOrder(50);
        ProductImageLayer roofColor = layer(
                11L,
                product,
                LayerType.ROOF_COLOR,
                "Antracit",
                null
        );
        when(layerRepository.findFirstByProductIdAndTypeAndOptionNameIgnoreCase(
                1L,
                LayerType.LAZURE,
                "Afromorsia"
        )).thenReturn(Optional.of(lazure));
        when(layerRepository.findFirstByProductIdAndTypeAndOptionNameIgnoreCase(
                1L,
                LayerType.ROOF_COLOR,
                "Antracit"
        )).thenReturn(Optional.of(roofColor));
        when(layerRepository.saveAndFlush(any(ProductImageLayer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        service.ensureDefaultVariants(product);
        assertEquals("Afromorsia", lazure.getOptionName());
        assertNull(lazure.getImageUrl());
        assertEquals(-1000, lazure.getSortOrder());
        assertTrue(lazure.isActive());
        assertNull(roofColor.getImageUrl());
        verify(fileStorageService).deleteFile("product-layers/old-afromorsia.webp");
        verifyNoInteractions(webpImageValidator);
    }
    @Test
    void createLayer_StoresValidatedFileAndPersistsNormalizedEntity() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(fileStorageService.storeProductLayer(validImage)).thenReturn("product-layers/a.webp");
        when(layerRepository.saveAndFlush(any(ProductImageLayer.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        ProductImageLayer created = service.createLayer(
                1L,
                LayerType.LAZURE,
                "  Kaštan  ",
                20,
                true,
                validImage
        );
        verify(webpImageValidator).validateProductLayer(validImage);
        assertEquals(product, created.getProduct());
        assertEquals(LayerType.LAZURE, created.getType());
        assertEquals("Kaštan", created.getOptionName());
        assertEquals("product-layers/a.webp", created.getImageUrl());
        assertEquals(20, created.getSortOrder());
        assertTrue(created.isActive());
    }
    @Test
    void createLayer_WhenDatabaseSaveFails_RemovesNewlyStoredFile() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(fileStorageService.storeProductLayer(validImage)).thenReturn("product-layers/a.webp");
        when(layerRepository.saveAndFlush(any(ProductImageLayer.class)))
                .thenThrow(new IllegalStateException("DB chyba"));
        assertThrows(
                IllegalStateException.class,
                () -> service.createLayer(1L, LayerType.LAZURE, "Kaštan", 0, true, validImage)
        );
        verify(fileStorageService).deleteFile("product-layers/a.webp");
    }
    @Test
    void createLayer_WithDuplicateName_RejectsBeforeFileUpload() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(layerRepository.existsByProductIdAndTypeAndOptionNameIgnoreCase(
                1L,
                LayerType.LAZURE,
                "Kaštan"
        )).thenReturn(true);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createLayer(1L, LayerType.LAZURE, "Kaštan", 0, true, validImage)
        );
        assertEquals(
                "Varianta s tímto názvem již pro vybraný produkt a typ existuje.",
                exception.getMessage()
        );
        verifyNoInteractions(webpImageValidator, fileStorageService);
        verify(layerRepository, never()).saveAndFlush(any());
    }
    @Test
    void createLayer_WithReservedDefaultName_RejectsWithoutUpload() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createLayer(
                        1L,
                        LayerType.LAZURE,
                        "Afromorsia",
                        0,
                        true,
                        validImage
                )
        );
        assertEquals(
                "Výchozí varianta Afromorsia vzniká automaticky a nepoužívá WebP soubor.",
                exception.getMessage()
        );
        verifyNoInteractions(webpImageValidator, fileStorageService);
        verify(layerRepository, never()).saveAndFlush(any());
    }
    @Test
    void createLayer_WithInvalidUpload_DoesNotStoreFileOrDatabaseRecord() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        doThrow(new IllegalArgumentException("Neplatný WebP soubor."))
                .when(webpImageValidator)
                .validateProductLayer(validImage);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.createLayer(1L, LayerType.LAZURE, "Kaštan", 0, true, validImage)
        );
        assertEquals("Neplatný WebP soubor.", exception.getMessage());
        verifyNoInteractions(fileStorageService);
        verify(layerRepository, never()).saveAndFlush(any());
    }
    @Test
    void updateLayer_WhenLayerBelongsToAnotherProduct_RejectsOperation() {
        Product otherProduct = Product.builder().id(2L).name("František").build();
        when(productRepository.findById(2L)).thenReturn(Optional.of(otherProduct));
        when(layerRepository.findByIdAndProductId(10L, 2L)).thenReturn(Optional.empty());
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateLayer(
                        2L,
                        10L,
                        LayerType.LAZURE,
                        "Kaštan",
                        0,
                        true,
                        null
                )
        );
        assertEquals(
                "Obrazová vrstva nebyla nalezena nebo nepatří zadanému produktu.",
                exception.getMessage()
        );
        verify(layerRepository, never()).saveAndFlush(any());
        verifyNoInteractions(fileStorageService);
    }
    @Test
    void updateLayer_WithReplacementFile_DeletesOldFileAfterSuccessfulSave() {
        ProductImageLayer existing = layer(
                10L,
                product,
                LayerType.LAZURE,
                "Kaštan",
                "product-layers/old.webp"
        );
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(layerRepository.findByIdAndProductId(10L, 1L)).thenReturn(Optional.of(existing));
        when(fileStorageService.storeProductLayer(validImage)).thenReturn("product-layers/new.webp");
        when(layerRepository.saveAndFlush(existing)).thenReturn(existing);
        ProductImageLayer updated = service.updateLayer(
                1L,
                10L,
                LayerType.LAZURE,
                "Kaštan",
                5,
                true,
                validImage
        );
        assertEquals("product-layers/new.webp", updated.getImageUrl());
        verify(fileStorageService).deleteFile("product-layers/old.webp");
        verify(fileStorageService, never()).deleteFile("product-layers/new.webp");
    }
    @Test
    void updateLayer_WhenLayerIsDefaultOption_RejectsOperation() {
        ProductImageLayer defaultLayer = layer(
                10L,
                product,
                LayerType.LAZURE,
                "Afromorsia",
                null
        );
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(layerRepository.findByIdAndProductId(10L, 1L)).thenReturn(Optional.of(defaultLayer));
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.updateLayer(
                        1L,
                        10L,
                        LayerType.LAZURE,
                        "Afromorsia",
                        -1000,
                        true,
                        null
                )
        );
        assertEquals("Výchozí variantu nelze upravit ani odstranit.", exception.getMessage());
        verify(layerRepository, never()).saveAndFlush(any());
        verifyNoInteractions(webpImageValidator, fileStorageService);
    }
    @Test
    void setLayerActive_UpdatesOnlyOwnedLayer() {
        ProductImageLayer existing = layer(
                10L,
                product,
                LayerType.ROOF_COLOR,
                "Červená",
                "product-layers/roof.webp"
        );
        existing.setActive(true);
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(layerRepository.findByIdAndProductId(10L, 1L)).thenReturn(Optional.of(existing));
        when(layerRepository.saveAndFlush(existing)).thenReturn(existing);
        ProductImageLayer result = service.setLayerActive(1L, 10L, false);
        assertFalse(result.isActive());
        verify(layerRepository).saveAndFlush(existing);
    }
    @Test
    void setLayerActive_WhenDefaultOptionWouldBeDeactivated_RejectsOperation() {
        ProductImageLayer defaultLayer = layer(
                10L,
                product,
                LayerType.ROOF_COLOR,
                "Antracit",
                null
        );
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(layerRepository.findByIdAndProductId(10L, 1L)).thenReturn(Optional.of(defaultLayer));
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.setLayerActive(1L, 10L, false)
        );
        assertEquals("Výchozí variantu nelze deaktivovat.", exception.getMessage());
        verify(layerRepository, never()).saveAndFlush(any());
    }
    @Test
    void deleteLayer_DeletesDatabaseRecordAndItsStoredFile() {
        ProductImageLayer existing = layer(
                10L,
                product,
                LayerType.ROOF_COLOR,
                "Červená",
                "product-layers/roof.webp"
        );
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(layerRepository.findByIdAndProductId(10L, 1L)).thenReturn(Optional.of(existing));
        service.deleteLayer(1L, 10L);
        verify(layerRepository).delete(existing);
        verify(layerRepository).flush();
        verify(fileStorageService).deleteFile("product-layers/roof.webp");
    }
    @Test
    void deleteLayer_WhenLayerIsDefaultOption_RejectsOperation() {
        ProductImageLayer defaultLayer = layer(
                10L,
                product,
                LayerType.LAZURE,
                "Afromorsia",
                null
        );
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(layerRepository.findByIdAndProductId(10L, 1L)).thenReturn(Optional.of(defaultLayer));
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.deleteLayer(1L, 10L)
        );
        assertEquals("Výchozí variantu nelze upravit ani odstranit.", exception.getMessage());
        verify(layerRepository, never()).delete(any());
        verifyNoInteractions(fileStorageService);
    }
    @Test
    void validateAndResolveSelection_ReturnsCanonicalActiveOptionName() {
        ProductImageLayer existing = layer(
                10L,
                product,
                LayerType.LAZURE,
                "Afromorsia",
                null
        );
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(layerRepository.existsByProductIdAndTypeAndActiveTrue(1L, LayerType.LAZURE))
                .thenReturn(true);
        when(layerRepository.findFirstByProductIdAndTypeAndActiveTrueAndOptionNameIgnoreCase(
                1L,
                LayerType.LAZURE,
                "afromorsia"
        )).thenReturn(Optional.of(existing));
        String result = service.validateAndResolveSelection(1L, LayerType.LAZURE, " afromorsia ");
        assertEquals("Afromorsia", result);
    }
    @Test
    void validateAndResolveSelection_WhenTypeHasNoOptions_AllowsNull() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(layerRepository.existsByProductIdAndTypeAndActiveTrue(1L, LayerType.ROOF_COLOR))
                .thenReturn(false);
        assertNull(service.validateAndResolveSelection(1L, LayerType.ROOF_COLOR, null));
    }
    @Test
    void validateAndResolveSelection_WhenActiveOptionsExist_RejectsMissingValue() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(layerRepository.existsByProductIdAndTypeAndActiveTrue(1L, LayerType.ROOF_COLOR))
                .thenReturn(true);
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> service.validateAndResolveSelection(1L, LayerType.ROOF_COLOR, null)
        );
        assertEquals("Vyberte dostupnou barvu střechy produktu.", exception.getMessage());
    }
    private ProductImageLayer layer(
            Long id,
            Product owner,
            LayerType type,
            String optionName,
            String imageUrl
    ) {
        return ProductImageLayer.builder()
                .id(id)
                .product(owner)
                .type(type)
                .optionName(optionName)
                .imageUrl(imageUrl)
                .sortOrder(0)
                .active(true)
                .build();
    }
}
