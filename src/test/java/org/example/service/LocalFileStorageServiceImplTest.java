package org.example.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class LocalFileStorageServiceImplTest {

    private LocalFileStorageServiceImpl storageService;

    private Path imagesDirectory;

    @TempDir
    Path tempDirectory;

    @BeforeEach
    void setUp() {
        imagesDirectory = tempDirectory.resolve(
                "images"
        );

        storageService =
                new LocalFileStorageServiceImpl(
                        tempDirectory.toString()
                );

        storageService.init();
    }

    @Test
    void init_CreatesImagesDirectory() {
        assertTrue(
                Files.isDirectory(imagesDirectory)
        );
    }

    @Test
    void storeFile_SavesFileSuccessfully()
            throws IOException {
        MockMultipartFile file =
                new MockMultipartFile(
                        "image",
                        "test-image.jpg",
                        "image/jpeg",
                        "test data".getBytes()
                );

        String savedName =
                storageService.storeFile(file);

        assertNotNull(savedName);
        assertTrue(savedName.endsWith(".jpg"));

        assertTrue(
                Files.exists(
                        imagesDirectory.resolve(
                                savedName
                        )
                )
        );

        assertEquals(
                "test data",
                Files.readString(
                        imagesDirectory.resolve(
                                savedName
                        )
                )
        );
    }

    @Test
    void storeFile_InvalidPathSequence_ThrowsException() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "image",
                        "../test.jpg",
                        "image/jpeg",
                        "data".getBytes()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.storeFile(file)
        );
    }

    @Test
    void storeFile_FileWithoutExtension_ThrowsException() {
        MockMultipartFile file =
                new MockMultipartFile(
                        "image",
                        "image-without-extension",
                        "image/jpeg",
                        "data".getBytes()
                );

        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.storeFile(file)
        );
    }

    @Test
    void deleteFile_RemovesFileFromDisk()
            throws IOException {
        String fileName = "to-delete.jpg";

        Path filePath = imagesDirectory.resolve(
                fileName
        );

        Files.createDirectories(imagesDirectory);
        Files.writeString(filePath, "data");

        assertTrue(Files.exists(filePath));

        storageService.deleteFile(fileName);

        assertFalse(Files.exists(filePath));
    }

    @Test
    void deleteFile_NonExistingFile_DoesNotThrow() {
        assertDoesNotThrow(
                () -> storageService.deleteFile(
                        "non-existing.jpg"
                )
        );
    }

    @Test
    void deleteFile_PathTraversal_ThrowsException() {
        assertThrows(
                IllegalArgumentException.class,
                () -> storageService.deleteFile(
                        "../outside.jpg"
                )
        );
    }
}