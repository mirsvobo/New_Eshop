package org.example.controller;

import org.example.model.InstallationImage;
import org.example.model.InstallationPost;
import org.example.model.TaxMode;
import org.example.repository.InstallationPostRepository;
import org.example.service.Cart;
import org.example.service.InstallationPostService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(AdminInstallationPostController.class)
class AdminInstallationPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstallationPostService installationPostService;

    @MockitoBean
    private InstallationPostRepository installationPostRepository;

    @MockitoBean(name = "cart")
    private Cart cart;

    @BeforeEach
    void setUp() {
        when(cart.getTaxMode()).thenReturn(TaxMode.STANDARD);
        when(cart.getTotalItems()).thenReturn(0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listPosts_RendersPostsOrderedByAssemblyDateDescending() throws Exception {
        InstallationPost olderPost = createPost(
                1L,
                "Montáž Dřevníku Klasik",
                "Dřevník Klasik",
                LocalDate.of(2026, 7, 11),
                true,
                List.of("realizace/1.webp")
        );

        InstallationPost newerPost = createPost(
                2L,
                "Montáž Dřevníku XXL",
                "Dřevník XXL",
                LocalDate.of(2026, 7, 18),
                false,
                List.of(
                        "realizace/6.webp",
                        "realizace/7.webp"
                )
        );

        given(installationPostRepository.findAll())
                .willReturn(List.of(olderPost, newerPost));

        MvcResult result = mockMvc.perform(get("/admin/installation-posts"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/installation-post-list"))
                .andExpect(model().attribute(
                        "posts",
                        List.of(newerPost, olderPost)
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "data-testid=\"installation-post-list\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Montáž Dřevníku XXL"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Dřevník XXL"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "18.07.2026"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Montáž Dřevníku Klasik"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "11.07.2026"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "/images/realizace/6.webp"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Aktivní"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Skrytý"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "/admin/installation-posts/create"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "/admin/installation-posts/edit/2"
                        )
                ))
                .andReturn();

        String html = result.getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertEquals(
                2,
                countOccurrences(
                        html,
                        "data-testid=\"installation-post-row\""
                ),
                "Každý montážní příspěvek musí mít vlastní řádek."
        );

        assertAppearsBefore(
                html,
                "Montáž Dřevníku XXL",
                "Montáž Dřevníku Klasik",
                "Novější příspěvek musí být zobrazen před starším."
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createForm_RendersEmptyActivePostAndMultipartForm() throws Exception {
        mockMvc.perform(get("/admin/installation-posts/create"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/installation-post-form.html"))
                .andExpect(model().attributeExists("installationPost"))
                .andExpect(model().attribute(
                        "installationPost",
                        hasProperty("active", is(true))
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "data-testid=\"installation-post-form.html\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "enctype=\"multipart/form-data\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "name=\"title\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "name=\"productName\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "name=\"assemblyDate\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "name=\"content\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "name=\"active\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "name=\"imageFiles\""
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString("multiple")
                ));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_RendersExistingPostAndItsImages() throws Exception {
        InstallationPost post = createPost(
                10L,
                "Montáž u zákazníka v Pardubicích",
                "Dřevník XXL",
                LocalDate.of(2026, 7, 20),
                true,
                List.of(
                        "existing-1.webp",
                        "existing-2.webp"
                )
        );

        given(installationPostRepository.findById(10L))
                .willReturn(Optional.of(post));

        MvcResult result = mockMvc.perform(
                        get("/admin/installation-posts/edit/10")
                )
                .andExpect(status().isOk())
                .andExpect(view().name("admin/installation-post-form.html"))
                .andExpect(model().attribute("installationPost", post))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Montáž u zákazníka v Pardubicích"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "Dřevník XXL"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "2026-07-20"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "/images/existing-1.webp"
                        )
                ))
                .andExpect(content().string(
                        org.hamcrest.Matchers.containsString(
                                "/images/existing-2.webp"
                        )
                ))
                .andReturn();

        String html = result.getResponse()
                .getContentAsString(StandardCharsets.UTF_8);

        assertEquals(
                2,
                countOccurrences(
                        html,
                        "data-testid=\"existing-installation-image\""
                ),
                "Formulář musí zobrazit všechny existující obrázky."
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void savePost_WithValidData_SavesPostAndRedirects() throws Exception {
        MockMultipartFile firstImage = new MockMultipartFile(
                "imageFiles",
                "montaz-1.jpg",
                "image/jpeg",
                "image-content-1".getBytes()
        );

        MockMultipartFile secondImage = new MockMultipartFile(
                "imageFiles",
                "montaz-2.webp",
                "image/webp",
                "image-content-2".getBytes()
        );

        mockMvc.perform(multipart("/admin/installation-posts/save")
                        .file(firstImage)
                        .file(secondImage)
                        .param("title", "Nová montáž")
                        .param("productName", "Dřevník XXL")
                        .param("assemblyDate", "2026-07-26")
                        .param(
                                "content",
                                "Popis dokončené montáže u zákazníka."
                        )
                        .param("active", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl(
                        "/admin/installation-posts"
                ))
                .andExpect(flash().attribute(
                        "success",
                        "Příspěvek byl úspěšně uložen."
                ));

        ArgumentCaptor<InstallationPost> postCaptor =
                ArgumentCaptor.forClass(InstallationPost.class);

        verify(installationPostService).savePostWithImages(
                postCaptor.capture(),
                anyList()
        );

        InstallationPost submittedPost = postCaptor.getValue();

        assertEquals("Nová montáž", submittedPost.getTitle());
        assertEquals(
                "Dřevník XXL",
                submittedPost.getProductName()
        );
        assertEquals(
                LocalDate.of(2026, 7, 26),
                submittedPost.getAssemblyDate()
        );
        assertEquals(
                "Popis dokončené montáže u zákazníka.",
                submittedPost.getContent()
        );
        assertTrue(submittedPost.isActive());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void savePost_WithInvalidData_ReturnsFormWithValidationErrors()
            throws Exception {

        mockMvc.perform(multipart("/admin/installation-posts/save")
                        .param("title", " ")
                        .param("productName", "")
                        .param("content", " ")
                        .param("active", "true")
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name(
                        "admin/installation-post-form.html"
                ))
                .andExpect(model().attributeHasFieldErrors(
                        "installationPost",
                        "title",
                        "productName",
                        "assemblyDate",
                        "content"
                ));

        verify(
                installationPostService,
                never()
        ).savePostWithImages(
                org.mockito.ArgumentMatchers.any(
                        InstallationPost.class
                ),
                org.mockito.ArgumentMatchers.anyList()
        );
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void installationPostAdministration_DeniesCustomerAccess()
            throws Exception {

        mockMvc.perform(get("/admin/installation-posts"))
                .andExpect(status().isForbidden());

        mockMvc.perform(get(
                        "/admin/installation-posts/create"
                ))
                .andExpect(status().isForbidden());

        mockMvc.perform(multipart(
                        "/admin/installation-posts/save"
                )
                        .param("title", "Zakázaná montáž")
                        .param("productName", "Dřevník XXL")
                        .param("assemblyDate", "2026-07-26")
                        .param("content", "Obsah příspěvku")
                        .param("active", "true")
                        .with(csrf()))
                .andExpect(status().isForbidden());

        verify(
                installationPostService,
                never()
        ).savePostWithImages(
                org.mockito.ArgumentMatchers.any(
                        InstallationPost.class
                ),
                org.mockito.ArgumentMatchers.anyList()
        );
    }

    private InstallationPost createPost(
            Long id,
            String title,
            String productName,
            LocalDate assemblyDate,
            boolean active,
            List<String> imageUrls
    ) {
        InstallationPost post = InstallationPost.builder()
                .id(id)
                .title(title)
                .productName(productName)
                .assemblyDate(assemblyDate)
                .content("Popis realizace " + title)
                .active(active)
                .images(new ArrayList<>())
                .build();

        for (int index = 0; index < imageUrls.size(); index++) {
            post.addImage(
                    InstallationImage.builder()
                            .id((long) index + 1)
                            .imageUrl(imageUrls.get(index))
                            .displayOrder(index)
                            .build()
            );
        }

        return post;
    }

    private int countOccurrences(
            String text,
            String searchedValue
    ) {
        int count = 0;
        int currentIndex = 0;

        while (
                (currentIndex = text.indexOf(
                        searchedValue,
                        currentIndex
                )) != -1
        ) {
            count++;
            currentIndex += searchedValue.length();
        }

        return count;
    }

    private void assertAppearsBefore(
            String html,
            String firstValue,
            String secondValue,
            String message
    ) {
        int firstIndex = html.indexOf(firstValue);
        int secondIndex = html.indexOf(secondValue);

        assertTrue(
                firstIndex >= 0,
                "V HTML chybí hodnota: " + firstValue
        );

        assertTrue(
                secondIndex >= 0,
                "V HTML chybí hodnota: " + secondValue
        );

        assertTrue(firstIndex < secondIndex, message);
    }
}