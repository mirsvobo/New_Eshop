package org.example.controller;

import org.example.model.InstallationPost;
import org.example.repository.InstallationPostRepository;
import org.example.service.InstallationPostService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AdminInstallationPostController.class)
public class AdminInstallationPostControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private InstallationPostService installationPostService;

    @MockBean
    private InstallationPostRepository installationPostRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    public void testSaveInstallationPostValidationAndSuccess() throws Exception {
        mockMvc.perform(multipart("/admin/installation-posts/save")
                        .file("imageFiles", "image_content_1".getBytes())
                        .file("imageFiles", "image_content_2".getBytes())
                        .param("title", "Nová montáž")
                        .param("productName", "Dřevník XXL")
                        .param("assemblyDate", "2026-07-26")
                        .param("content", "Popis nové montáže...")
                        .param("active", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/installation-posts"));

        verify(installationPostService).savePostWithImages(any(InstallationPost.class), any(List.class));
    }
}