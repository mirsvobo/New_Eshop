package org.example.controller;
import org.example.model.LayerType;
import org.example.model.Product;
import org.example.model.ProductImageLayer;
import org.example.model.User;
import org.example.repository.ProductRepository;
import org.example.repository.TaxRateRepository;
import org.example.repository.UserRepository;
import org.example.service.Cart;
import org.example.service.ProductImageLayerService;
import org.example.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.data.domain.Sort;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.mock.web.MockMultipartFile;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
@WebMvcTest(AdminProductController.class)
class AdminProductControllerTest {
    @Autowired
    private MockMvc mockMvc;
    @MockitoBean
    private ProductRepository productRepository;
    @MockitoBean
    private ProductService productService;
    @MockitoBean
    private UserRepository userRepository;
    @MockitoBean
    private TaxRateRepository taxRateRepository;
    @MockitoBean
    private ProductImageLayerService productImageLayerService;
    @MockitoBean(name = "cart")
    private Cart cart;
    private User adminUser;
    @BeforeEach
    void setUp() {
        adminUser = User.builder()
                .email("admin@test.cz")
                .firstName("Admin")
                .build();
        given(userRepository.findByEmail("admin@test.cz")).willReturn(Optional.of(adminUser));
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void shouldReturnListViewWithFilteredProductsWhenListProductsCalled() throws Exception {
        given(productRepository.findFilteredProducts(anyString(), any(), any(), any(Sort.class)))
                .willReturn(Collections.emptyList());
        mockMvc.perform(get("/admin/produkty")
                        .param("search", "test")
                        .param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/produkty"));
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void shouldReturnFormWithErrorsWhenValidationFails() throws Exception {
        mockMvc.perform(post("/admin/produkty/save")
                        .param("name", "Testovací produkt")
                        .param("price", "chybna_hodnota")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(model().hasErrors());
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void showAddProductForm_ShouldReturnForm() throws Exception {
        given(taxRateRepository.findAll()).willReturn(Collections.emptyList());
        mockMvc.perform(get("/admin/produkty/novy"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(model().attributeExists("product"))
                .andExpect(model().attributeExists("taxes"));
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void showEditProductForm_ShouldReturnForm() throws Exception {
        Product product = new Product();
        product.setId(1L);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(taxRateRepository.findAll()).willReturn(Collections.emptyList());
        mockMvc.perform(get("/admin/produkty/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(model().attribute("product", product))
                .andExpect(model().attributeExists("taxes"));
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void showEditProductForm_RendersProtectedDefaultsWithoutUploadControls() throws Exception {
        Product product = Product.builder()
                .id(1L)
                .name("Dřevník Martin")
                .type(Product.ProductType.PRODUCT)
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
        ProductImageLayer defaultRoofColor = ProductImageLayer.builder()
                .id(12L)
                .product(product)
                .type(LayerType.ROOF_COLOR)
                .optionName("Antracit")
                .imageUrl(null)
                .sortOrder(-1000)
                .active(true)
                .build();
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(taxRateRepository.findAll()).willReturn(Collections.emptyList());
        given(productImageLayerService.getLayersForProduct(1L))
                .willReturn(List.of(defaultLazure, defaultRoofColor));
        mockMvc.perform(get("/admin/produkty/edit/1"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-form"))
                .andExpect(content().string(containsString("data-testid=\"default-product-variant\"")))
                .andExpect(content().string(containsString("Afromorsia")))
                .andExpect(content().string(containsString("Antracit")))
                .andExpect(content().string(containsString("Nevyžaduje WebP soubor.")))
                .andExpect(content().string(containsString("Výchozí varianta")))
                .andExpect(content().string(not(containsString("/vrstvy/11/upravit"))))
                .andExpect(content().string(not(containsString("/vrstvy/12/upravit"))));
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void saveProduct_Success_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/admin/produkty/save")
                        .param("name", "Valid Product")
                        .param("price", "100.00")
                        .param("stockQuantity", "10")
                        .param("minStockLevel", "2")
                        .param("unit", "ks")
                        .param("type", "PRODUCT")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty"))
                .andExpect(flash().attributeExists("success"));
        verify(productService).saveProduct(any(Product.class), eq(null), any(User.class));
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void saveProduct_WithDimensions_Success_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/admin/produkty/save")
                        .param("name", "Valid Product with Dimensions")
                        .param("price", "100.00")
                        .param("stockQuantity", "10")
                        .param("minStockLevel", "2")
                        .param("unit", "ks")
                        .param("type", "PRODUCT")
                        .param("width", "120.5")
                        .param("depth", "80.0")
                        .param("height", "200.0")
                        .param("volume", "1.92")
                        .param("additionalDimensions", "Přesah střechy: 10 cm")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty"))
                .andExpect(flash().attributeExists("success"));
        ArgumentCaptor<Product> productCaptor = ArgumentCaptor.forClass(Product.class);
        verify(productService).saveProduct(productCaptor.capture(), eq(null), any(User.class));
        Product capturedProduct = productCaptor.getValue();
        assertEquals(120.5, capturedProduct.getWidth());
        assertEquals(80.0, capturedProduct.getDepth());
        assertEquals(200.0, capturedProduct.getHeight());
        assertEquals(1.92, capturedProduct.getVolume());
        assertEquals("Přesah střechy: 10 cm", capturedProduct.getAdditionalDimensions());
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void manageRecipe_ShouldReturnRecipeView() throws Exception {
        Product product = new Product();
        product.setId(1L);
        given(productRepository.findById(1L)).willReturn(Optional.of(product));
        given(productService.getAllMaterials()).willReturn(Collections.emptyList());
        mockMvc.perform(get("/admin/produkty/1/kusovnik"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/product-recipe"))
                .andExpect(model().attribute("product", product))
                .andExpect(model().attributeExists("materials"));
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void addRecipeItem_Success_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/admin/produkty/1/kusovnik/pridat")
                        .param("materialId", "2")
                        .param("quantity", "5")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty/1/kusovnik"))
                .andExpect(flash().attributeExists("success"));
        verify(productService).addRecipeItem(eq(1L), eq(2L), eq(5), any(User.class));
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void deleteRecipeItem_Success_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/admin/produkty/1/kusovnik/smazat/2")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty/1/kusovnik"))
                .andExpect(flash().attributeExists("success"));
        verify(productService).deleteRecipeItem(eq(1L), eq(2L), any(User.class));
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void deleteProduct_Success_ShouldRedirect() throws Exception {
        mockMvc.perform(post("/admin/produkty/delete/1").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty"))
                .andExpect(flash().attributeExists("success"));
        verify(productService).deleteProduct(eq(1L), any());
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void listProducts_ShouldReturnView() throws Exception {
        given(productRepository.findFilteredProducts(any(), any(), any(), any())).willReturn(java.util.Collections.emptyList());
        mockMvc.perform(get("/admin/produkty"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/produkty"))
                .andExpect(model().attributeExists("products"));
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void createImageLayer_DelegatesValidatedParametersToService() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "layerImageFile",
                "kastan.webp",
                "image/webp",
                new byte[]{1, 2, 3}
        );
        mockMvc.perform(multipart("/admin/produkty/1/vrstvy")
                        .file(imageFile)
                        .param("type", "LAZURE")
                        .param("optionName", "Kaštan")
                        .param("sortOrder", "10")
                        .param("active", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty/edit/1#obrazove-varianty"))
                .andExpect(flash().attribute("success", "Obrazová varianta byla přidána."));
        verify(productImageLayerService).createLayer(
                1L,
                LayerType.LAZURE,
                "Kaštan",
                10,
                true,
                imageFile
        );
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void deleteImageLayer_DelegatesProductAndLayerOwnershipToService() throws Exception {
        mockMvc.perform(post("/admin/produkty/1/vrstvy/9/smazat").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty/edit/1#obrazove-varianty"))
                .andExpect(flash().attribute("success", "Obrazová varianta byla odstraněna."));
        verify(productImageLayerService).deleteLayer(1L, 9L);
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void deleteImageLayer_WhenDefaultVariantIsProtected_ShowsCzechError() throws Exception {
        doThrow(new IllegalArgumentException("Výchozí variantu nelze upravit ani odstranit."))
                .when(productImageLayerService)
                .deleteLayer(1L, 11L);
        mockMvc.perform(post("/admin/produkty/1/vrstvy/11/smazat").with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/produkty/edit/1#obrazove-varianty"))
                .andExpect(flash().attribute(
                        "error",
                        "Výchozí variantu nelze upravit ani odstranit."
                ));
    }
    @Test
    @WithMockUser(username = "admin@test.cz", roles = "ADMIN")
    void createImageLayer_WhenServiceRejectsUpload_ShowsCzechError() throws Exception {
        MockMultipartFile imageFile = new MockMultipartFile(
                "layerImageFile",
                "vrstva.png",
                "image/png",
                new byte[]{1, 2, 3}
        );
        org.mockito.Mockito.doThrow(new IllegalArgumentException("Vrstva musí být ve formátu WebP s příponou .webp."))
                .when(productImageLayerService)
                .createLayer(1L, LayerType.LAZURE, "Dub", 0, true, imageFile);
        mockMvc.perform(multipart("/admin/produkty/1/vrstvy")
                        .file(imageFile)
                        .param("type", "LAZURE")
                        .param("optionName", "Dub")
                        .param("sortOrder", "0")
                        .param("active", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attribute(
                        "error",
                        "Vrstva musí být ve formátu WebP s příponou .webp."
                ));
    }
}
