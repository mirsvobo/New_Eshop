package org.example.service;

import org.example.model.InstallationImage;
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
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstallationPostAuditTest {

    private static final String MODULE_NAME =
            "MONTÁŽE";

    @Mock
    private InstallationPostRepository
            installationPostRepository;

    @Mock
    private FileStorageService
            fileStorageService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private InstallationPostService
            installationPostService;

    @Test
    void savePostWithImages_NewPost_LogsCreation() {
        InstallationPost newPost =
                createPost(
                        null,
                        "Nová montáž",
                        true
                );

        when(
                installationPostRepository.save(
                        newPost
                )
        ).thenAnswer(invocation -> {
            InstallationPost savedPost =
                    invocation.getArgument(0);

            savedPost.setId(1L);

            return savedPost;
        });

        InstallationPost result =
                installationPostService
                        .savePostWithImages(
                                newPost,
                                List.of()
                        );

        assertSame(
                newPost,
                result
        );

        InOrder inOrder = inOrder(
                installationPostRepository,
                auditService
        );

        inOrder.verify(
                installationPostRepository
        ).save(newPost);

        inOrder.verify(
                auditService
        ).log(
                MODULE_NAME,
                "VYTVOŘENÍ",
                "Vytvořen příspěvek z montáže: "
                        + "Nová montáž."
        );
    }

    @Test
    void savePostWithImages_ExistingPost_LogsUpdate() {
        InstallationPost existingPost =
                createPost(
                        10L,
                        "Původní montáž",
                        true
                );

        InstallationPost submittedPost =
                createPost(
                        10L,
                        "Upravená montáž",
                        false
                );

        submittedPost.setContent(
                "Nový popis montáže."
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
                installationPostService
                        .savePostWithImages(
                                submittedPost,
                                List.of()
                        );

        assertSame(
                existingPost,
                result
        );

        assertFalse(
                existingPost.isActive()
        );

        InOrder inOrder = inOrder(
                installationPostRepository,
                auditService
        );

        inOrder.verify(
                installationPostRepository
        ).findById(10L);

        inOrder.verify(
                installationPostRepository
        ).save(existingPost);

        inOrder.verify(
                auditService
        ).log(
                MODULE_NAME,
                "ÚPRAVA",
                "Upraven příspěvek z montáže: "
                        + "Upravená montáž."
        );
    }

    @Test
    void toggleActive_ActivePost_LogsHiding() {
        InstallationPost existingPost =
                createPost(
                        20L,
                        "Montáž v Brně",
                        true
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
                installationPostService
                        .toggleActive(20L);

        assertFalse(result.isActive());

        InOrder inOrder = inOrder(
                installationPostRepository,
                auditService
        );

        inOrder.verify(
                installationPostRepository
        ).save(existingPost);

        inOrder.verify(
                auditService
        ).log(
                MODULE_NAME,
                "SKRYTÍ",
                "Příspěvek z montáže "
                        + "'Montáž v Brně' byl skryt."
        );
    }

    @Test
    void toggleActive_InactivePost_LogsPublishing() {
        InstallationPost existingPost =
                createPost(
                        21L,
                        "Montáž v Ostravě",
                        false
                );

        when(
                installationPostRepository.findById(
                        21L
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
                installationPostService
                        .toggleActive(21L);

        assertTrue(result.isActive());

        InOrder inOrder = inOrder(
                installationPostRepository,
                auditService
        );

        inOrder.verify(
                installationPostRepository
        ).save(existingPost);

        inOrder.verify(
                auditService
        ).log(
                MODULE_NAME,
                "ZVEŘEJNĚNÍ",
                "Příspěvek z montáže "
                        + "'Montáž v Ostravě' byl zveřejněn."
        );
    }

    @Test
    void deleteImage_LogsDeletedImage() {
        InstallationPost existingPost =
                createPost(
                        30L,
                        "Montáž v Praze",
                        true
                );

        InstallationImage image =
                InstallationImage.builder()
                        .id(301L)
                        .imageUrl(
                                "montaz-praha.webp"
                        )
                        .displayOrder(0)
                        .build();

        existingPost.addImage(image);

        when(
                installationPostRepository.findById(
                        30L
                )
        ).thenReturn(
                Optional.of(existingPost)
        );

        when(
                installationPostRepository.save(
                        existingPost
                )
        ).thenReturn(existingPost);

        installationPostService.deleteImage(
                30L,
                301L
        );

        InOrder inOrder = inOrder(
                installationPostRepository,
                fileStorageService,
                auditService
        );

        inOrder.verify(
                installationPostRepository
        ).findById(30L);

        inOrder.verify(
                installationPostRepository
        ).save(existingPost);

        inOrder.verify(
                installationPostRepository
        ).flush();

        inOrder.verify(
                fileStorageService
        ).deleteFile(
                "montaz-praha.webp"
        );

        inOrder.verify(
                auditService
        ).log(
                MODULE_NAME,
                "SMAZÁNÍ FOTOGRAFIE",
                "Z příspěvku z montáže "
                        + "'Montáž v Praze' byla smazána "
                        + "fotografie: montaz-praha.webp."
        );
    }

    @Test
    void deletePost_LogsPostAndImageCount() {
        InstallationPost existingPost =
                createPost(
                        40L,
                        "Montáž v Plzni",
                        true
                );

        existingPost.addImage(
                InstallationImage.builder()
                        .id(401L)
                        .imageUrl("plzen-1.jpg")
                        .displayOrder(0)
                        .build()
        );

        existingPost.addImage(
                InstallationImage.builder()
                        .id(402L)
                        .imageUrl("plzen-2.webp")
                        .displayOrder(1)
                        .build()
        );

        when(
                installationPostRepository.findById(
                        40L
                )
        ).thenReturn(
                Optional.of(existingPost)
        );

        installationPostService.deletePost(40L);

        InOrder inOrder = inOrder(
                installationPostRepository,
                fileStorageService,
                auditService
        );

        inOrder.verify(
                installationPostRepository
        ).findById(40L);

        inOrder.verify(
                installationPostRepository
        ).delete(existingPost);

        inOrder.verify(
                installationPostRepository
        ).flush();

        inOrder.verify(
                fileStorageService
        ).deleteFile("plzen-1.jpg");

        inOrder.verify(
                fileStorageService
        ).deleteFile("plzen-2.webp");

        inOrder.verify(
                auditService
        ).log(
                MODULE_NAME,
                "SMAZÁNÍ",
                "Smazán příspěvek z montáže: "
                        + "Montáž v Plzni "
                        + "(fotografií: 2)."
        );
    }

    @Test
    void savePostWithImages_RepositoryFails_DoesNotWriteAuditLog() {
        InstallationPost newPost =
                createPost(
                        null,
                        "Neuložená montáž",
                        true
                );

        when(
                installationPostRepository.save(
                        newPost
                )
        ).thenThrow(
                new RuntimeException(
                        "Databáze není dostupná."
                )
        );

        assertThrows(
                RuntimeException.class,
                () -> installationPostService
                        .savePostWithImages(
                                newPost,
                                List.of()
                        )
        );

        verifyNoInteractions(auditService);
    }

    @Test
    void toggleActive_PostDoesNotExist_DoesNotWriteAuditLog() {
        when(
                installationPostRepository.findById(
                        999L
                )
        ).thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> installationPostService
                        .toggleActive(999L)
        );

        verify(
                installationPostRepository,
                never()
        ).save(any());

        verifyNoInteractions(auditService);
    }

    private InstallationPost createPost(
            Long id,
            String title,
            boolean active
    ) {
        return InstallationPost.builder()
                .id(id)
                .title(title)
                .productName("Dřevník XXL")
                .assemblyDate(
                        LocalDate.of(
                                2026,
                                8,
                                20
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