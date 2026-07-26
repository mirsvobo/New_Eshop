package org.example.service;

import org.example.model.InstallationImage;
import org.example.model.InstallationPost;
import org.example.repository.InstallationPostRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class InstallationPostServiceTest {

    @Mock
    private InstallationPostRepository installationPostRepository;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private InstallationPostService installationPostService;

    @Test
    public void testSavePostWithImages() {
        InstallationPost post = InstallationPost.builder()
                .title("Montáž Dřevníku")
                .productName("Dřevník Klasik")
                .assemblyDate(LocalDate.of(2026, 7, 26))
                .content("Testovací obsah příspěvku.")
                .active(true)
                .images(new ArrayList<>())
                .build();

        MockMultipartFile file1 = new MockMultipartFile("images", "image1.jpg", "image/jpeg", "image_data_1".getBytes());
        MockMultipartFile file2 = new MockMultipartFile("images", "image2.png", "image/png", "image_data_2".getBytes());
        List<MultipartFile> imageFiles = Arrays.asList(file1, file2);

        when(fileStorageService.storeFile(file1)).thenReturn("uuid1.jpg");
        when(fileStorageService.storeFile(file2)).thenReturn("uuid2.png");

        when(installationPostRepository.save(any(InstallationPost.class))).thenAnswer(invocation -> {
            InstallationPost savedPost = invocation.getArgument(0);
            savedPost.setId(1L);
            return savedPost;
        });

        InstallationPost savedPost = installationPostService.savePostWithImages(post, imageFiles);

        assertNotNull(savedPost, "Uložený příspěvek nesmí být null");
        assertEquals(1L, savedPost.getId());
        assertEquals("Montáž Dřevníku", savedPost.getTitle());

        List<InstallationImage> images = savedPost.getImages();
        assertNotNull(images);
        assertEquals(2, images.size(), "Příspěvek musí obsahovat přesně 2 obrázky");

        InstallationImage img1 = images.get(0);
        assertEquals("uuid1.jpg", img1.getImageUrl());
        assertEquals(0, img1.getDisplayOrder(), "První obrázek musí mít pořadí 0");
        assertEquals(savedPost, img1.getInstallationPost(), "Obrázek musí mít správnou vazbu na příspěvek");

        InstallationImage img2 = images.get(1);
        assertEquals("uuid2.png", img2.getImageUrl());
        assertEquals(1, img2.getDisplayOrder(), "Druhý obrázek musí mít pořadí 1");
        assertEquals(savedPost, img2.getInstallationPost(), "Obrázek musí mít správnou vazbu na příspěvek");

        verify(fileStorageService, times(2)).storeFile(any(MultipartFile.class));

        ArgumentCaptor<InstallationPost> postCaptor = ArgumentCaptor.forClass(InstallationPost.class);
        verify(installationPostRepository, times(1)).save(postCaptor.capture());

        InstallationPost capturedPost = postCaptor.getValue();
        assertEquals(2, capturedPost.getImages().size());
    }
}