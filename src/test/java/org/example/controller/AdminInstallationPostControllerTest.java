package org.example.controller;

import org.example.config.SecurityConfig;
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
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        AdminInstallationPostController.class
)
@Import(SecurityConfig.class)
class AdminInstallationPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private InstallationPostService
            installationPostService;

    @MockitoBean
    private InstallationPostRepository
            installationPostRepository;

    @MockitoBean(name = "cart")
    private Cart cart;

    @BeforeEach
    void setUp() {
        when(cart.getTaxMode())
                .thenReturn(TaxMode.STANDARD);

        when(cart.getTotalItems())
                .thenReturn(0);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listPosts_RendersPostsSortedNewestFirst()
            throws Exception {
        InstallationPost olderPost =
                createPost(
                        1L,
                        "Starší montáž",
                        LocalDate.of(
                                2026,
                                6,
                                10
                        )
                );

        InstallationPost newerPost =
                createPost(
                        2L,
                        "Novější montáž",
                        LocalDate.of(
                                2026,
                                7,
                                20
                        )
                );

        when(installationPostRepository.findAll())
                .thenReturn(
                        List.of(
                                olderPost,
                                newerPost
                        )
                );

        mockMvc.perform(
                        get(
                                "/admin/installation-posts"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        view().name(
                                "admin/installation-post-list"
                        )
                )
                .andExpect(
                        model().attribute(
                                "posts",
                                List.of(
                                        newerPost,
                                        olderPost
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "data-testid=\"installation-post-list\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Novější montáž"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Starší montáž"
                                )
                        )
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void createForm_RendersEmptyActivePostAndMultipartForm()
            throws Exception {
        mockMvc.perform(
                        get(
                                "/admin/installation-posts/create"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        view().name(
                                "admin/installation-post-form"
                        )
                )
                .andExpect(
                        model().attributeExists(
                                "installationPost"
                        )
                )
                .andExpect(
                        model().attribute(
                                "installationPost",
                                hasProperty(
                                        "active",
                                        is(true)
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "data-testid=\"installation-post-form\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "enctype=\"multipart/form-data\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "name=\"title\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "name=\"productName\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "name=\"assemblyDate\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "name=\"content\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "name=\"imageFiles\""
                                )
                        )
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void editForm_RendersExistingPostAndItsImages()
            throws Exception {
        InstallationPost post =
                createPost(
                        10L,
                        "Montáž u zákazníka v Pardubicích",
                        LocalDate.of(
                                2026,
                                7,
                                20
                        )
                );

        post.setProductName("Dřevník XXL");
        post.setContent(
                "Popis realizace Montáž u zákazníka v Pardubicích"
        );

        InstallationImage firstImage =
                InstallationImage.builder()
                        .id(1L)
                        .imageUrl(
                                "existing-1.webp"
                        )
                        .displayOrder(0)
                        .build();

        InstallationImage secondImage =
                InstallationImage.builder()
                        .id(2L)
                        .imageUrl(
                                "existing-2.webp"
                        )
                        .displayOrder(1)
                        .build();

        post.addImage(firstImage);
        post.addImage(secondImage);

        when(
                installationPostRepository.findById(
                        10L
                )
        ).thenReturn(Optional.of(post));

        mockMvc.perform(
                        get(
                                "/admin/installation-posts/edit/10"
                        )
                )
                .andExpect(status().isOk())
                .andExpect(
                        view().name(
                                "admin/installation-post-form"
                        )
                )
                .andExpect(
                        model().attribute(
                                "installationPost",
                                post
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "value=\"10\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Montáž u zákazníka v Pardubicích"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "value=\"2026-07-20\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/images/existing-1.webp"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/images/existing-2.webp"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "data-testid=\"existing-installation-image\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/admin/installation-posts/10/images/1/delete"
                                )
                        )
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void savePost_WithValidData_SavesPostAndRedirects()
            throws Exception {
        MockMultipartFile firstImage =
                new MockMultipartFile(
                        "imageFiles",
                        "installation-1.jpg",
                        "image/jpeg",
                        "first-image".getBytes()
                );

        MockMultipartFile secondImage =
                new MockMultipartFile(
                        "imageFiles",
                        "installation-2.webp",
                        "image/webp",
                        "second-image".getBytes()
                );

        mockMvc.perform(
                        multipart(
                                "/admin/installation-posts/save"
                        )
                                .file(firstImage)
                                .file(secondImage)
                                .param(
                                        "title",
                                        "Nová montáž"
                                )
                                .param(
                                        "productName",
                                        "Dřevník XXL"
                                )
                                .param(
                                        "assemblyDate",
                                        "2026-07-26"
                                )
                                .param(
                                        "content",
                                        "Popis nové montáže."
                                )
                                .param(
                                        "active",
                                        "true"
                                )
                                .with(csrf())
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/admin/installation-posts"
                        )
                )
                .andExpect(
                        flash().attribute(
                                "success",
                                "Příspěvek byl úspěšně uložen."
                        )
                );

        ArgumentCaptor<InstallationPost> postCaptor =
                ArgumentCaptor.forClass(
                        InstallationPost.class
                );

        verify(installationPostService)
                .savePostWithImages(
                        postCaptor.capture(),
                        anyList()
                );

        InstallationPost capturedPost =
                postCaptor.getValue();

        assertEquals(
                "Nová montáž",
                capturedPost.getTitle()
        );

        assertEquals(
                "Dřevník XXL",
                capturedPost.getProductName()
        );

        assertEquals(
                LocalDate.of(
                        2026,
                        7,
                        26
                ),
                capturedPost.getAssemblyDate()
        );

        assertEquals(
                "Popis nové montáže.",
                capturedPost.getContent()
        );

        assertTrue(capturedPost.isActive());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void savePost_WithInvalidData_ReturnsFormWithValidationErrors()
            throws Exception {
        mockMvc.perform(
                        multipart(
                                "/admin/installation-posts/save"
                        )
                                .param("title", " ")
                                .param("productName", "")
                                .param("content", " ")
                                .param("active", "true")
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(
                        view().name(
                                "admin/installation-post-form"
                        )
                )
                .andExpect(
                        model().attributeHasFieldErrors(
                                "installationPost",
                                "title",
                                "productName",
                                "assemblyDate",
                                "content"
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Název příspěvku je povinný."
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Název produktu je povinný."
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Datum montáže je povinné."
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Popis montáže je povinný."
                                )
                        )
                );

        verify(
                installationPostService,
                never()
        ).savePostWithImages(
                any(InstallationPost.class),
                anyList()
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deletePost_UsesServiceAndRedirects()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/admin/installation-posts/delete/15"
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/admin/installation-posts"
                        )
                )
                .andExpect(
                        flash().attribute(
                                "success",
                                "Příspěvek a jeho fotografie byly smazány."
                        )
                );

        verify(installationPostService)
                .deletePost(15L);

        verify(
                installationPostRepository,
                never()
        ).deleteById(15L);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void deleteImage_UsesServiceAndReturnsToEditForm()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/admin/installation-posts/20/images/201/delete"
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().is3xxRedirection()
                )
                .andExpect(
                        redirectedUrl(
                                "/admin/installation-posts/edit/20"
                        )
                )
                .andExpect(
                        flash().attribute(
                                "success",
                                "Fotografie byla odstraněna."
                        )
                );

        verify(installationPostService)
                .deleteImage(
                        20L,
                        201L
                );
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void installationPostAdministration_DeniesCustomerAccess()
            throws Exception {
        mockMvc.perform(
                        get(
                                "/admin/installation-posts"
                        )
                )
                .andExpect(
                        status().isForbidden()
                );

        mockMvc.perform(
                        get(
                                "/admin/installation-posts/create"
                        )
                )
                .andExpect(
                        status().isForbidden()
                );

        mockMvc.perform(
                        get(
                                "/admin/installation-posts/edit/10"
                        )
                )
                .andExpect(
                        status().isForbidden()
                );

        mockMvc.perform(
                        multipart(
                                "/admin/installation-posts/save"
                        )
                                .param(
                                        "title",
                                        "Zakázaný příspěvek"
                                )
                                .param(
                                        "productName",
                                        "Dřevník"
                                )
                                .param(
                                        "assemblyDate",
                                        "2026-07-26"
                                )
                                .param(
                                        "content",
                                        "Zakázaný obsah."
                                )
                                .with(csrf())
                )
                .andExpect(
                        status().isForbidden()
                );

        mockMvc.perform(
                        post(
                                "/admin/installation-posts/delete/10"
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isForbidden()
                );

        mockMvc.perform(
                        post(
                                "/admin/installation-posts/10/images/1/delete"
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                installationPostService,
                never()
        ).savePostWithImages(
                any(InstallationPost.class),
                anyList()
        );

        verify(
                installationPostService,
                never()
        ).deletePost(any());

        verify(
                installationPostService,
                never()
        ).deleteImage(
                any(),
                any()
        );
    }

    private InstallationPost createPost(
            Long id,
            String title,
            LocalDate assemblyDate
    ) {
        return InstallationPost.builder()
                .id(id)
                .title(title)
                .productName("Dřevník Klasik")
                .assemblyDate(assemblyDate)
                .content(
                        "Popis realizace " + title
                )
                .active(true)
                .images(new ArrayList<>())
                .build();
    }
}