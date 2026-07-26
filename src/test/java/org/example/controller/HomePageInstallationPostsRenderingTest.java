package org.example.controller;

import org.example.model.InstallationImage;
import org.example.model.InstallationPost;
import org.example.model.TaxMode;
import org.example.repository.InstallationPostRepository;
import org.example.service.Cart;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
class HomePageInstallationPostsRenderingTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean(name = "cart")
    private Cart cart;

    @MockitoBean
    private InstallationPostRepository installationPostRepository;

    @BeforeEach
    void setUp() {
        when(cart.getTaxMode()).thenReturn(TaxMode.STANDARD);
        when(cart.getTotalItems()).thenReturn(0);
    }

    @Test
    void homePage_RendersInstallationPostsWithMetadataContentAndImages() throws Exception {
        InstallationPost xxlPost = createPost(
                2L,
                "Montáž Dřevníku XXL",
                "Dřevník XXL",
                LocalDate.of(2026, 7, 18),
                "Prostorný dřevník jsme usadili na připravený betonový podklad.",
                List.of(
                        "realizace/6.webp",
                        "realizace/7.webp",
                        "realizace/8.webp",
                        "realizace/9.webp"
                )
        );

        InstallationPost klasikPost = createPost(
                1L,
                "Montáž Dřevníku Klasik",
                "Dřevník Klasik",
                LocalDate.of(2026, 7, 11),
                "Kompaktní realizace pro menší zahradu.",
                List.of("realizace/1.webp")
        );

        when(installationPostRepository.findAllByActiveTrueOrderByAssemblyDateDesc())
                .thenReturn(List.of(xxlPost, klasikPost));

        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(containsString("Živě z montáží")))
                .andExpect(content().string(containsString("Montáž Dřevníku XXL")))
                .andExpect(content().string(containsString("Dřevník XXL")))
                .andExpect(content().string(containsString("18.07.2026")))
                .andExpect(content().string(containsString(
                        "Prostorný dřevník jsme usadili na připravený betonový podklad."
                )))
                .andExpect(content().string(containsString("Montáž Dřevníku Klasik")))
                .andExpect(content().string(containsString("Dřevník Klasik")))
                .andExpect(content().string(containsString("11.07.2026")))
                .andExpect(content().string(containsString(
                        "Kompaktní realizace pro menší zahradu."
                )))
                .andExpect(content().string(containsString("/images/realizace/6.webp")))
                .andExpect(content().string(containsString("/images/realizace/7.webp")))
                .andExpect(content().string(containsString("/images/realizace/8.webp")))
                .andExpect(content().string(containsString("/images/realizace/9.webp")))
                .andExpect(content().string(containsString("/images/realizace/1.webp")))
                .andReturn();

        String html = result.getResponse().getContentAsString(StandardCharsets.UTF_8);

        assertEquals(
                1,
                countOccurrences(html, "data-testid=\"installation-posts-section\""),
                "Homepage musí obsahovat právě jednu sekci Živě z montáží."
        );

        assertEquals(
                2,
                countOccurrences(html, "data-testid=\"installation-post-card\""),
                "Každý montážní příspěvek musí být vykreslen jako samostatná karta."
        );

        assertEquals(
                2,
                countOccurrences(html, "data-testid=\"installation-post-gallery\""),
                "Každý příspěvek musí mít vlastní galerii."
        );

        assertEquals(
                5,
                countOccurrences(html, "data-testid=\"installation-post-image\""),
                "Musí se vykreslit všechny obrázky obou příspěvků."
        );

        assertAppearsBefore(
                html,
                "Montáž Dřevníku XXL",
                "Montáž Dřevníku Klasik",
                "Novější příspěvek musí být zobrazen před starším příspěvkem."
        );

        assertAppearsBefore(
                html,
                "/images/realizace/6.webp",
                "/images/realizace/7.webp",
                "Obrázky musí zachovat pořadí displayOrder."
        );

        assertAppearsBefore(
                html,
                "/images/realizace/7.webp",
                "/images/realizace/8.webp",
                "Obrázky musí zachovat pořadí displayOrder."
        );

        assertAppearsBefore(
                html,
                "/images/realizace/8.webp",
                "/images/realizace/9.webp",
                "Obrázky musí zachovat pořadí displayOrder."
        );
    }

    @Test
    void homePage_DoesNotRenderInstallationSectionWhenThereAreNoPosts() throws Exception {
        when(installationPostRepository.findAllByActiveTrueOrderByAssemblyDateDesc())
                .thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(content().string(
                        not(containsString("data-testid=\"installation-posts-section\""))
                ))
                .andExpect(content().string(not(containsString("Živě z montáží"))));
    }

    private InstallationPost createPost(
            Long id,
            String title,
            String productName,
            LocalDate assemblyDate,
            String postContent,
            List<String> imageUrls
    ) {
        InstallationPost post = InstallationPost.builder()
                .id(id)
                .title(title)
                .productName(productName)
                .assemblyDate(assemblyDate)
                .content(postContent)
                .active(true)
                .images(new ArrayList<>())
                .build();

        for (int index = 0; index < imageUrls.size(); index++) {
            post.addImage(
                    InstallationImage.builder()
                            .imageUrl(imageUrls.get(index))
                            .displayOrder(index)
                            .build()
            );
        }

        return post;
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