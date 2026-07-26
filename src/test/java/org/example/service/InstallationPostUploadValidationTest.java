package org.example.service;

import org.example.model.InstallationImage;
import org.example.model.InstallationPost;
import org.example.repository.InstallationPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InstallationPostUploadValidationTest {

    private static final int MAX_IMAGES_PER_POST = 12;

    private static final int MAX_IMAGE_SIZE_BYTES =
            10 * 1024 * 1024;

    @Mock
    private InstallationPostRepository
            installationPostRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private InstallationPostService
            installationPostService;

    @Test
    void savePostWithImages_UnsupportedContentType_ThrowsException() {
        InstallationPost post = createNewPost();

        MockMultipartFile pdfFile =
                new MockMultipartFile(
                        "imageFiles",
                        "document.pdf",
                        "application/pdf",
                        "not-an-image".getBytes()
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> installationPostService
                                .savePostWithImages(
                                        post,
                                        List.of(pdfFile)
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains("JPG, PNG nebo WEBP")
        );

        verifyNoInteractions(fileStorageService);

        verify(
                installationPostRepository,
                never()
        ).save(any());
    }

    @Test
    void savePostWithImages_FileLargerThanTenMegabytes_ThrowsException() {
        InstallationPost post = createNewPost();

        MockMultipartFile oversizedFile =
                new MockMultipartFile(
                        "imageFiles",
                        "large-image.jpg",
                        "image/jpeg",
                        new byte[
                                MAX_IMAGE_SIZE_BYTES + 1
                                ]
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> installationPostService
                                .savePostWithImages(
                                        post,
                                        List.of(
                                                oversizedFile
                                        )
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains("10 MB")
        );

        verifyNoInteractions(fileStorageService);

        verify(
                installationPostRepository,
                never()
        ).save(any());
    }

    @Test
    void savePostWithImages_MoreThanTwelveImagesForNewPost_ThrowsException() {
        InstallationPost post = createNewPost();

        List<MultipartFile> imageFiles =
                createValidImageFiles(
                        MAX_IMAGES_PER_POST + 1
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> installationPostService
                                .savePostWithImages(
                                        post,
                                        imageFiles
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains("12")
        );

        verifyNoInteractions(fileStorageService);

        verify(
                installationPostRepository,
                never()
        ).save(any());
    }

    @Test
    void savePostWithImages_ExistingAndNewImagesExceedLimit_ThrowsException() {
        InstallationPost existingPost =
                createExistingPostWithImages(
                        11
                );

        InstallationPost submittedPost =
                InstallationPost.builder()
                        .id(existingPost.getId())
                        .title("Upravená montáž")
                        .productName("Dřevník XXL")
                        .assemblyDate(
                                LocalDate.of(
                                        2026,
                                        8,
                                        10
                                )
                        )
                        .content(
                                "Upravený popis montáže."
                        )
                        .active(true)
                        .images(new ArrayList<>())
                        .build();

        List<MultipartFile> newImages =
                createValidImageFiles(2);

        when(
                installationPostRepository.findById(
                        existingPost.getId()
                )
        ).thenReturn(
                Optional.of(existingPost)
        );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> installationPostService
                                .savePostWithImages(
                                        submittedPost,
                                        newImages
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains("12")
        );

        verifyNoInteractions(fileStorageService);

        verify(
                installationPostRepository,
                never()
        ).save(any());
    }

    @Test
    void savePostWithImages_ContentTypeDoesNotMatchExtension_ThrowsException() {
        InstallationPost post = createNewPost();

        MockMultipartFile mismatchedFile =
                new MockMultipartFile(
                        "imageFiles",
                        "photo.jpg",
                        "image/png",
                        "fake-png-data".getBytes()
                );

        IllegalArgumentException exception =
                assertThrows(
                        IllegalArgumentException.class,
                        () -> installationPostService
                                .savePostWithImages(
                                        post,
                                        List.of(
                                                mismatchedFile
                                        )
                                )
                );

        assertTrue(
                exception.getMessage()
                        .contains("neodpovídá")
        );

        verifyNoInteractions(fileStorageService);

        verify(
                installationPostRepository,
                never()
        ).save(any());
    }

    @Test
    void savePostWithImages_OneFileInBatchIsInvalid_DoesNotStoreAnyFile() {
        InstallationPost post = createNewPost();

        MockMultipartFile validFile =
                new MockMultipartFile(
                        "imageFiles",
                        "valid.jpg",
                        "image/jpeg",
                        "valid-image".getBytes()
                );

        MockMultipartFile invalidFile =
                new MockMultipartFile(
                        "imageFiles",
                        "invalid.exe",
                        "application/octet-stream",
                        "invalid-file".getBytes()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> installationPostService
                        .savePostWithImages(
                                post,
                                List.of(
                                        validFile,
                                        invalidFile
                                )
                        )
        );

        /*
         * Celá dávka musí být ověřena před uložením
         * prvního souboru. Neplatná dávka proto
         * nesmí vytvořit ani jeden fyzický soubor.
         */
        verifyNoInteractions(fileStorageService);

        verify(
                installationPostRepository,
                never()
        ).save(any());
    }

    @Test
    void savePostWithImages_EmptyFilesDoNotCountTowardsLimit() {
        InstallationPost post = createNewPost();

        List<MultipartFile> imageFiles =
                new ArrayList<>(
                        createValidImageFiles(
                                MAX_IMAGES_PER_POST
                        )
                );

        imageFiles.add(
                new MockMultipartFile(
                        "imageFiles",
                        "",
                        "application/octet-stream",
                        new byte[0]
                )
        );

        for (int index = 0;
             index < MAX_IMAGES_PER_POST;
             index++) {

            when(
                    fileStorageService.storeFile(
                            imageFiles.get(index)
                    )
            ).thenReturn(
                    "stored-" + index + ".jpg"
            );
        }

        when(
                installationPostRepository.save(
                        any(InstallationPost.class)
                )
        ).thenAnswer(
                invocation ->
                        invocation.getArgument(0)
        );

        installationPostService.savePostWithImages(
                post,
                imageFiles
        );

        assertTrue(
                post.getImages().size()
                        == MAX_IMAGES_PER_POST
        );

        verify(
                installationPostRepository
        ).save(post);
    }

    private InstallationPost createNewPost() {
        return InstallationPost.builder()
                .title("Nová montáž")
                .productName("Dřevník XXL")
                .assemblyDate(
                        LocalDate.of(
                                2026,
                                8,
                                1
                        )
                )
                .content(
                        "Popis nové montáže."
                )
                .active(true)
                .images(new ArrayList<>())
                .build();
    }

    private InstallationPost createExistingPostWithImages(
            int imageCount
    ) {
        InstallationPost post =
                InstallationPost.builder()
                        .id(50L)
                        .title("Existující montáž")
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

        for (int index = 0;
             index < imageCount;
             index++) {

            InstallationImage image =
                    InstallationImage.builder()
                            .id((long) index + 1)
                            .imageUrl(
                                    "existing-"
                                            + index
                                            + ".webp"
                            )
                            .displayOrder(index)
                            .build();

            post.addImage(image);
        }

        return post;
    }

    private List<MultipartFile> createValidImageFiles(
            int count
    ) {
        List<MultipartFile> imageFiles =
                new ArrayList<>();

        for (int index = 0;
             index < count;
             index++) {

            imageFiles.add(
                    new MockMultipartFile(
                            "imageFiles",
                            "image-" + index + ".jpg",
                            "image/jpeg",
                            (
                                    "image-data-"
                                            + index
                            ).getBytes()
                    )
            );
        }

        return imageFiles;
    }
}