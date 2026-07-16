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

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp() {
        storageService = new LocalFileStorageServiceImpl(tempDir.toString());
        storageService.init();
    }

    @Test
    void storeFile_SavesFileSuccessfully() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "image", "test-image.jpg", "image/jpeg", "test data".getBytes()
        );

        String savedName = storageService.storeFile(file);

        assertNotNull(savedName);
        assertTrue(savedName.endsWith(".jpg"));
        assertTrue(Files.exists(tempDir.resolve(savedName)));
    }

    @Test
    void storeFile_InvalidPathSequence_ThrowsException() {
        MockMultipartFile file = new MockMultipartFile(
                "image", "../test.jpg", "image/jpeg", "data".getBytes()
        );

        assertThrows(RuntimeException.class, () -> storageService.storeFile(file));
    }

    @Test
    void deleteFile_RemovesFileFromDisk() throws IOException {
        String fileName = "to-delete.jpg";
        Path filePath = tempDir.resolve(fileName);
        Files.write(filePath, "data".getBytes());
        assertTrue(Files.exists(filePath));

        storageService.deleteFile(fileName);

        assertFalse(Files.exists(filePath));
    }
}