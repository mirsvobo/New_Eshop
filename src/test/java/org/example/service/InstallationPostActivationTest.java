package org.example.service;

import org.example.model.InstallationPost;
import org.example.repository.InstallationPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstallationPostActivationTest {

    @Mock
    private InstallationPostRepository
            installationPostRepository;

    @Mock
    private FileStorageService
            fileStorageService;

    @InjectMocks
    private InstallationPostService
            installationPostService;

    @Test
    void toggleActive_ActivePost_DeactivatesAndSavesPost() {
        InstallationPost existingPost =
                createPost(
                        10L,
                        true
                );

        when(
                installationPostRepository.findById(
                        10L
                )
        ).thenReturn(
                Optional.of(existingPost)
        );

        when(
                installationPostRepository.save(
                        existingPost
                )
        ).thenReturn(existingPost);

        InstallationPost result =
                installationPostService.toggleActive(
                        10L
                );

        assertFalse(
                existingPost.isActive(),
                "Aktivní příspěvek se musí skrýt."
        );

        assertSame(
                existingPost,
                result,
                "Service má vrátit uložený příspěvek."
        );

        InOrder inOrder = inOrder(
                installationPostRepository
        );

        inOrder.verify(
                installationPostRepository
        ).findById(10L);

        inOrder.verify(
                installationPostRepository
        ).save(existingPost);

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void toggleActive_InactivePost_ActivatesAndSavesPost() {
        InstallationPost existingPost =
                createPost(
                        20L,
                        false
                );

        when(
                installationPostRepository.findById(
                        20L
                )
        ).thenReturn(
                Optional.of(existingPost)
        );

        when(
                installationPostRepository.save(
                        existingPost
                )
        ).thenReturn(existingPost);

        InstallationPost result =
                installationPostService.toggleActive(
                        20L
                );

        assertTrue(
                existingPost.isActive(),
                "Neaktivní příspěvek se musí zveřejnit."
        );

        assertSame(
                existingPost,
                result
        );

        verify(
                installationPostRepository
        ).save(existingPost);

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void toggleActive_PostDoesNotExist_ThrowsException() {
        when(
                installationPostRepository.findById(
                        999L
                )
        ).thenReturn(Optional.empty());

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> installationPostService
                                .toggleActive(999L)
                );

        assertTrue(
                exception.getMessage()
                        .contains("999")
        );

        verify(
                installationPostRepository
        ).findById(999L);

        verify(
                installationPostRepository,
                never()
        ).save(
                org.mockito.ArgumentMatchers.any()
        );

        verifyNoInteractions(fileStorageService);
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