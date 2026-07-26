package org.example.service;

import org.example.model.InstallationImage;
import org.example.model.InstallationPost;
import org.example.repository.InstallationPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InstallationPostServiceTest {

    @Mock
    private InstallationPostRepository installationPostRepository;

    @Mock
    private FileStorageService fileStorageService;

    @Mock
    private AuditService auditService;

    @InjectMocks
    private InstallationPostService installationPostService;

    @Test
    void savePostWithImages_NewPost_SavesImagesFromDisplayOrderZero() {
        InstallationPost newPost = InstallationPost.builder()
                .title("Montáž Dřevníku")
                .productName("Dřevník Klasik")
                .assemblyDate(LocalDate.of(2026, 7, 26))
                .content("Testovací obsah příspěvku.")
                .active(true)
                .images(new ArrayList<>())
                .build();

        MockMultipartFile firstFile = new MockMultipartFile(
                "imageFiles",
                "image1.jpg",
                "image/jpeg",
                "image-data-1".getBytes()
        );

        MockMultipartFile secondFile = new MockMultipartFile(
                "imageFiles",
                "image2.png",
                "image/png",
                "image-data-2".getBytes()
        );

        when(fileStorageService.storeFile(firstFile))
                .thenReturn("uuid1.jpg");

        when(fileStorageService.storeFile(secondFile))
                .thenReturn("uuid2.png");

        when(installationPostRepository.save(any(InstallationPost.class)))
                .thenAnswer(invocation -> {
                    InstallationPost savedPost = invocation.getArgument(0);
                    savedPost.setId(1L);
                    return savedPost;
                });

        InstallationPost savedPost =
                installationPostService.savePostWithImages(
                        newPost,
                        List.of(firstFile, secondFile)
                );

        assertNotNull(savedPost);
        assertEquals(1L, savedPost.getId());
        assertEquals("Montáž Dřevníku", savedPost.getTitle());

        assertNotNull(savedPost.getImages());
        assertEquals(2, savedPost.getImages().size());

        InstallationImage firstImage = savedPost.getImages().get(0);

        assertEquals("uuid1.jpg", firstImage.getImageUrl());
        assertEquals(0, firstImage.getDisplayOrder());
        assertSame(savedPost, firstImage.getInstallationPost());

        InstallationImage secondImage = savedPost.getImages().get(1);

        assertEquals("uuid2.png", secondImage.getImageUrl());
        assertEquals(1, secondImage.getDisplayOrder());
        assertSame(savedPost, secondImage.getInstallationPost());

        verify(fileStorageService).storeFile(firstFile);
        verify(fileStorageService).storeFile(secondFile);
        verify(installationPostRepository).save(newPost);
    }

    @Test
    void savePostWithImages_ExistingPostWithoutNewFiles_PreservesExistingImages() {
        InstallationPost existingPost = createExistingPost(10L);

        InstallationImage firstExistingImage = addImage(
                existingPost,
                101L,
                "existing-1.webp",
                0
        );

        InstallationImage secondExistingImage = addImage(
                existingPost,
                102L,
                "existing-2.webp",
                1
        );

        InstallationPost submittedPost = InstallationPost.builder()
                .id(10L)
                .title("Upravený název montáže")
                .productName("Dřevník XXL")
                .assemblyDate(LocalDate.of(2026, 8, 5))
                .content("Aktualizovaný popis montáže.")
                .active(false)
                .images(new ArrayList<>())
                .build();

        lenient().when(installationPostRepository.findById(10L))
                .thenReturn(Optional.of(existingPost));

        when(installationPostRepository.save(any(InstallationPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InstallationPost savedPost =
                installationPostService.savePostWithImages(
                        submittedPost,
                        List.of()
                );

        assertSame(
                existingPost,
                savedPost,
                "Při editaci se musí uložit původní spravovaná entita z databáze."
        );

        assertEquals("Upravený název montáže", savedPost.getTitle());
        assertEquals("Dřevník XXL", savedPost.getProductName());
        assertEquals(
                LocalDate.of(2026, 8, 5),
                savedPost.getAssemblyDate()
        );
        assertEquals(
                "Aktualizovaný popis montáže.",
                savedPost.getContent()
        );
        assertFalse(savedPost.isActive());

        assertEquals(
                2,
                savedPost.getImages().size(),
                "Editace bez nových souborů nesmí odstranit původní obrázky."
        );

        assertSame(firstExistingImage, savedPost.getImages().get(0));
        assertSame(secondExistingImage, savedPost.getImages().get(1));

        assertSame(
                existingPost,
                firstExistingImage.getInstallationPost()
        );

        assertSame(
                existingPost,
                secondExistingImage.getInstallationPost()
        );

        verify(installationPostRepository).findById(10L);
        verify(installationPostRepository).save(existingPost);
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void savePostWithImages_ExistingPostWithNewFiles_AppendsImagesAfterHighestDisplayOrder() {
        InstallationPost existingPost = createExistingPost(20L);

        InstallationImage firstExistingImage = addImage(
                existingPost,
                201L,
                "existing-1.webp",
                0
        );

        InstallationImage secondExistingImage = addImage(
                existingPost,
                202L,
                "existing-2.webp",
                2
        );

        InstallationPost submittedPost = InstallationPost.builder()
                .id(20L)
                .title("Montáž po úpravě")
                .productName("Dřevník L")
                .assemblyDate(LocalDate.of(2026, 8, 10))
                .content("Doplněné informace o realizaci.")
                .active(true)
                .images(new ArrayList<>())
                .build();

        MockMultipartFile firstNewFile = new MockMultipartFile(
                "imageFiles",
                "new-1.jpg",
                "image/jpeg",
                "new-image-data-1".getBytes()
        );

        MockMultipartFile secondNewFile = new MockMultipartFile(
                "imageFiles",
                "new-2.webp",
                "image/webp",
                "new-image-data-2".getBytes()
        );

        lenient().when(installationPostRepository.findById(20L))
                .thenReturn(Optional.of(existingPost));

        when(fileStorageService.storeFile(firstNewFile))
                .thenReturn("stored-new-1.jpg");

        when(fileStorageService.storeFile(secondNewFile))
                .thenReturn("stored-new-2.webp");

        when(installationPostRepository.save(any(InstallationPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InstallationPost savedPost =
                installationPostService.savePostWithImages(
                        submittedPost,
                        List.of(firstNewFile, secondNewFile)
                );

        assertSame(existingPost, savedPost);

        assertEquals(
                4,
                savedPost.getImages().size(),
                "Původní obrázky musí zůstat a nové se k nim musí přidat."
        );

        assertSame(firstExistingImage, savedPost.getImages().get(0));
        assertSame(secondExistingImage, savedPost.getImages().get(1));

        InstallationImage firstNewImage =
                savedPost.getImages().get(2);

        assertEquals(
                "stored-new-1.jpg",
                firstNewImage.getImageUrl()
        );
        assertEquals(
                3,
                firstNewImage.getDisplayOrder(),
                "Nové pořadí musí navázat na nejvyšší existující displayOrder."
        );
        assertSame(existingPost, firstNewImage.getInstallationPost());

        InstallationImage secondNewImage =
                savedPost.getImages().get(3);

        assertEquals(
                "stored-new-2.webp",
                secondNewImage.getImageUrl()
        );
        assertEquals(4, secondNewImage.getDisplayOrder());
        assertSame(existingPost, secondNewImage.getInstallationPost());

        verify(installationPostRepository).findById(20L);
        verify(fileStorageService).storeFile(firstNewFile);
        verify(fileStorageService).storeFile(secondNewFile);
        verify(installationPostRepository).save(existingPost);
    }

    @Test
    void savePostWithImages_ExistingPostDoesNotExist_ThrowsException() {
        InstallationPost submittedPost = InstallationPost.builder()
                .id(999L)
                .title("Neexistující příspěvek")
                .productName("Dřevník XXL")
                .assemblyDate(LocalDate.of(2026, 8, 15))
                .content("Obsah příspěvku.")
                .active(true)
                .images(new ArrayList<>())
                .build();

        lenient().when(installationPostRepository.findById(999L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> installationPostService.savePostWithImages(
                        submittedPost,
                        List.of()
                )
        );

        verify(installationPostRepository).findById(999L);
        verify(installationPostRepository, never()).save(any());
        verifyNoInteractions(fileStorageService);
    }

    @Test
    void savePostWithImages_IgnoresEmptyFiles() {
        InstallationPost newPost = InstallationPost.builder()
                .title("Montáž bez fotografie")
                .productName("Dřevník Kompakt")
                .assemblyDate(LocalDate.of(2026, 8, 20))
                .content("Příspěvek bez nahrané fotografie.")
                .active(true)
                .images(new ArrayList<>())
                .build();

        MockMultipartFile emptyFile = new MockMultipartFile(
                "imageFiles",
                "",
                "application/octet-stream",
                new byte[0]
        );

        when(installationPostRepository.save(any(InstallationPost.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        InstallationPost savedPost =
                installationPostService.savePostWithImages(
                        newPost,
                        List.of(emptyFile)
                );

        assertTrue(savedPost.getImages().isEmpty());

        verifyNoInteractions(fileStorageService);
        verify(installationPostRepository).save(newPost);
    }

    private InstallationPost createExistingPost(Long id) {
        return InstallationPost.builder()
                .id(id)
                .title("Původní název")
                .productName("Dřevník Klasik")
                .assemblyDate(LocalDate.of(2026, 7, 20))
                .content("Původní popis realizace.")
                .active(true)
                .images(new ArrayList<>())
                .build();
    }

    private InstallationImage addImage(
            InstallationPost post,
            Long imageId,
            String imageUrl,
            int displayOrder
    ) {
        InstallationImage image = InstallationImage.builder()
                .id(imageId)
                .imageUrl(imageUrl)
                .displayOrder(displayOrder)
                .build();

        post.addImage(image);
        return image;
    }

    @Test
    void deletePost_ExistingPost_DeletesDatabaseEntityAndAllPhysicalFiles() {
        InstallationPost existingPost = createExistingPost(30L);

        addImage(
                existingPost,
                301L,
                "first-image.webp",
                0
        );

        addImage(
                existingPost,
                302L,
                "second-image.jpg",
                1
        );

        when(installationPostRepository.findById(30L))
                .thenReturn(Optional.of(existingPost));

        installationPostService.deletePost(30L);

        InOrder inOrder = inOrder(
                installationPostRepository,
                fileStorageService
        );

        inOrder.verify(installationPostRepository)
                .delete(existingPost);

        inOrder.verify(installationPostRepository)
                .flush();

        inOrder.verify(fileStorageService)
                .deleteFile("first-image.webp");

        inOrder.verify(fileStorageService)
                .deleteFile("second-image.jpg");
    }

    @Test
    void deletePost_PostDoesNotExist_ThrowsException() {
        when(installationPostRepository.findById(999L))
                .thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> installationPostService.deletePost(999L)
        );

        assertTrue(
                exception.getMessage().contains("999")
        );

        verify(installationPostRepository)
                .findById(999L);

        verify(installationPostRepository, never())
                .delete(any());

        verify(installationPostRepository, never())
                .flush();

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void deleteImage_ExistingImage_RemovesImageFileAndResequencesRemainingImages() {
        InstallationPost existingPost = createExistingPost(40L);

        InstallationImage firstImage = addImage(
                existingPost,
                401L,
                "first.webp",
                0
        );

        InstallationImage imageToDelete = addImage(
                existingPost,
                402L,
                "delete-me.webp",
                3
        );

        InstallationImage lastImage = addImage(
                existingPost,
                403L,
                "last.webp",
                7
        );

        when(installationPostRepository.findById(40L))
                .thenReturn(Optional.of(existingPost));

        when(installationPostRepository.save(existingPost))
                .thenReturn(existingPost);

        installationPostService.deleteImage(
                40L,
                402L
        );

        assertEquals(
                2,
                existingPost.getImages().size()
        );

        assertSame(
                firstImage,
                existingPost.getImages().get(0)
        );

        assertSame(
                lastImage,
                existingPost.getImages().get(1)
        );

        assertEquals(
                0,
                firstImage.getDisplayOrder()
        );

        assertEquals(
                1,
                lastImage.getDisplayOrder()
        );

        assertNull(
                imageToDelete.getInstallationPost(),
                "Odstraněný obrázek už nesmí odkazovat na příspěvek."
        );

        InOrder inOrder = inOrder(
                installationPostRepository,
                fileStorageService
        );

        inOrder.verify(installationPostRepository)
                .save(existingPost);

        inOrder.verify(installationPostRepository)
                .flush();

        inOrder.verify(fileStorageService)
                .deleteFile("delete-me.webp");
    }

    @Test
    void deleteImage_ImageDoesNotBelongToPost_ThrowsException() {
        InstallationPost existingPost = createExistingPost(50L);

        addImage(
                existingPost,
                501L,
                "existing.webp",
                0
        );

        when(installationPostRepository.findById(50L))
                .thenReturn(Optional.of(existingPost));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> installationPostService.deleteImage(
                        50L,
                        999L
                )
        );

        assertTrue(
                exception.getMessage().contains("999")
        );

        verify(installationPostRepository)
                .findById(50L);

        verify(installationPostRepository, never())
                .save(any());

        verify(installationPostRepository, never())
                .flush();

        verifyNoInteractions(fileStorageService);
    }

    @Test
    void deleteImage_PostDoesNotExist_ThrowsException() {
        when(installationPostRepository.findById(888L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> installationPostService.deleteImage(
                        888L,
                        1L
                )
        );

        verify(installationPostRepository)
                .findById(888L);

        verify(installationPostRepository, never())
                .save(any());

        verifyNoInteractions(fileStorageService);
    }
}