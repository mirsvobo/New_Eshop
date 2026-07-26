package org.example.controller;

import org.example.config.SecurityConfig;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.ArrayList;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        AdminInstallationPostController.class
)
@Import(SecurityConfig.class)
class AdminInstallationPostActivationTest {

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
    void toggleActive_ActivePost_HidesPostAndRedirects()
            throws Exception {
        InstallationPost hiddenPost =
                createPost(
                        10L,
                        false
                );

        when(
                installationPostService.toggleActive(
                        10L
                )
        ).thenReturn(hiddenPost);

        mockMvc.perform(
                        post(
                                "/admin/installation-posts/10/toggle-active"
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
                                "Příspěvek byl skryt."
                        )
                );

        verify(installationPostService)
                .toggleActive(10L);

        verifyNoInteractions(
                installationPostRepository
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void toggleActive_InactivePost_PublishesPostAndRedirects()
            throws Exception {
        InstallationPost publishedPost =
                createPost(
                        20L,
                        true
                );

        when(
                installationPostService.toggleActive(
                        20L
                )
        ).thenReturn(publishedPost);

        mockMvc.perform(
                        post(
                                "/admin/installation-posts/20/toggle-active"
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
                                "Příspěvek byl zveřejněn."
                        )
                );

        verify(installationPostService)
                .toggleActive(20L);

        verifyNoInteractions(
                installationPostRepository
        );
    }

    @Test
    @WithMockUser(roles = "CUSTOMER")
    void toggleActive_CustomerAccess_IsForbidden()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/admin/installation-posts/30/toggle-active"
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                installationPostService,
                never()
        ).toggleActive(30L);

        verifyNoInteractions(
                installationPostRepository
        );
    }

    @Test
    @WithMockUser(roles = "EMPLOYEE")
    void toggleActive_EmployeeAccess_IsForbidden()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/admin/installation-posts/40/toggle-active"
                        )
                                .with(csrf())
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                installationPostService,
                never()
        ).toggleActive(40L);

        verifyNoInteractions(
                installationPostRepository
        );
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void toggleActive_WithoutCsrf_IsForbidden()
            throws Exception {
        mockMvc.perform(
                        post(
                                "/admin/installation-posts/50/toggle-active"
                        )
                )
                .andExpect(
                        status().isForbidden()
                );

        verify(
                installationPostService,
                never()
        ).toggleActive(50L);

        verifyNoInteractions(
                installationPostRepository
        );
    }

    private InstallationPost createPost(
            Long id,
            boolean active
    ) {
        return InstallationPost.builder()
                .id(id)
                .title("Montáž v Brně")
                .productName("Dřevník XXL")
                .assemblyDate(
                        LocalDate.of(
                                2026,
                                8,
                                15
                        )
                )
                .content(
                        "Popis dokončené montáže."
                )
                .active(active)
                .images(new ArrayList<>())
                .build();
    }
}