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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@WebMvcTest(
        AdminInstallationPostController.class
)
@Import(SecurityConfig.class)
class AdminInstallationPostUploadValidationTest {

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
    void savePost_InvalidUpload_ReturnsFormWithErrorMessage()
            throws Exception {
        MockMultipartFile invalidFile =
                new MockMultipartFile(
                        "imageFiles",
                        "document.pdf",
                        "application/pdf",
                        "invalid-content".getBytes()
                );

        doThrow(
                new IllegalArgumentException(
                        "Povolené jsou pouze JPG, PNG nebo WEBP."
                )
        ).when(installationPostService)
                .savePostWithImages(
                        any(InstallationPost.class),
                        anyList()
                );

        mockMvc.perform(
                        multipart(
                                "/admin/installation-posts/save"
                        )
                                .file(invalidFile)
                                .param(
                                        "title",
                                        "Montáž v Praze"
                                )
                                .param(
                                        "productName",
                                        "Dřevník XXL"
                                )
                                .param(
                                        "assemblyDate",
                                        "2026-08-10"
                                )
                                .param(
                                        "content",
                                        "Popis montáže v Praze."
                                )
                                .param(
                                        "active",
                                        "true"
                                )
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(
                        view().name(
                                "admin/installation-post-form"
                        )
                )
                .andExpect(
                        model().attribute(
                                "error",
                                "Povolené jsou pouze JPG, PNG nebo WEBP."
                        )
                )
                .andExpect(
                        model().attribute(
                                "installationPost",
                                hasProperty(
                                        "title",
                                        is("Montáž v Praze")
                                )
                        )
                )
                .andExpect(
                        model().attribute(
                                "installationPost",
                                hasProperty(
                                        "productName",
                                        is("Dřevník XXL")
                                )
                        )
                )
                .andExpect(
                        model().attribute(
                                "installationPost",
                                hasProperty(
                                        "assemblyDate",
                                        is(
                                                LocalDate.of(
                                                        2026,
                                                        8,
                                                        10
                                                )
                                        )
                                )
                        )
                )
                .andExpect(
                        model().attribute(
                                "installationPost",
                                hasProperty(
                                        "content",
                                        is(
                                                "Popis montáže v Praze."
                                        )
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "Povolené jsou pouze JPG, PNG nebo WEBP."
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "value=\"Mont&aacute;ž v Praze\""
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "value=\"2026-08-10\""
                                )
                        )
                );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void saveExistingPost_InvalidUpload_PreservesExistingImages()
            throws Exception {
        InstallationPost existingPost =
                InstallationPost.builder()
                        .id(10L)
                        .title("Původní montáž")
                        .productName("Dřevník XXL")
                        .assemblyDate(
                                LocalDate.of(
                                        2026,
                                        7,
                                        20
                                )
                        )
                        .content(
                                "Původní popis montáže."
                        )
                        .active(true)
                        .images(new ArrayList<>())
                        .build();

        InstallationImage firstImage =
                InstallationImage.builder()
                        .id(101L)
                        .imageUrl(
                                "existing-first.webp"
                        )
                        .displayOrder(0)
                        .build();

        InstallationImage secondImage =
                InstallationImage.builder()
                        .id(102L)
                        .imageUrl(
                                "existing-second.jpg"
                        )
                        .displayOrder(1)
                        .build();

        existingPost.addImage(firstImage);
        existingPost.addImage(secondImage);

        when(
                installationPostRepository.findById(
                        10L
                )
        ).thenReturn(
                Optional.of(existingPost)
        );

        doThrow(
                new IllegalArgumentException(
                        "Fotografie je příliš velká. "
                                + "Maximální povolená velikost je 10 MB."
                )
        ).when(installationPostService)
                .savePostWithImages(
                        any(InstallationPost.class),
                        anyList()
                );

        MockMultipartFile oversizedFile =
                new MockMultipartFile(
                        "imageFiles",
                        "large-photo.jpg",
                        "image/jpeg",
                        "test-content".getBytes()
                );

        mockMvc.perform(
                        multipart(
                                "/admin/installation-posts/save"
                        )
                                .file(oversizedFile)
                                .param(
                                        "id",
                                        "10"
                                )
                                .param(
                                        "title",
                                        "Upravená montáž"
                                )
                                .param(
                                        "productName",
                                        "Dřevník XXL"
                                )
                                .param(
                                        "assemblyDate",
                                        "2026-08-11"
                                )
                                .param(
                                        "content",
                                        "Upravený popis montáže."
                                )
                                .param(
                                        "active",
                                        "true"
                                )
                                .with(csrf())
                )
                .andExpect(status().isOk())
                .andExpect(
                        view().name(
                                "admin/installation-post-form"
                        )
                )
                .andExpect(
                        model().attribute(
                                "error",
                                containsString("10 MB")
                        )
                )
                .andExpect(
                        model().attribute(
                                "installationPost",
                                hasProperty(
                                        "id",
                                        is(10L)
                                )
                        )
                )
                .andExpect(
                        model().attribute(
                                "installationPost",
                                hasProperty(
                                        "title",
                                        is("Upravená montáž")
                                )
                        )
                )
                .andExpect(
                        model().attribute(
                                "installationPost",
                                hasProperty(
                                        "images",
                                        hasSize(2)
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/images/existing-first.webp"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/images/existing-second.jpg"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/admin/installation-posts/10/images/101/delete"
                                )
                        )
                )
                .andExpect(
                        content().string(
                                containsString(
                                        "/admin/installation-posts/10/images/102/delete"
                                )
                        )
                );
    }
}