package org.example.service;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Slf4j
@Service
public class LocalFileStorageServiceImpl
        implements FileStorageService {

    private static final String PRODUCT_LAYER_DIRECTORY = "product-layers";

    private final Path fileStorageLocation;

    public LocalFileStorageServiceImpl(
            @Value(
                    "${app.storage.local-dir:./local-storage/}"
            )
            String baseStorageDirectory
    ) {
        this.fileStorageLocation = Paths
                .get(baseStorageDirectory)
                .resolve("images")
                .toAbsolutePath()
                .normalize();
    }

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(
                    fileStorageLocation
            );

            log.info(
                    "Initialized image storage directory at: {}",
                    fileStorageLocation
            );
        } catch (IOException exception) {
            log.error(
                    "Could not create the directory where images will be stored.",
                    exception
            );

            throw new IllegalStateException(
                    "Nepodařilo se vytvořit adresář pro ukládání obrázků.",
                    exception
            );
        }
    }

    @Override
    public String storeFile(MultipartFile file) {
        String extension = validateAndGetExtension(file, null);
        return storeWithGeneratedName(file, extension, null);
    }

    @Override
    public String storeProductLayer(MultipartFile file) {
        String extension = validateAndGetExtension(file, "webp");
        return storeWithGeneratedName(file, extension, PRODUCT_LAYER_DIRECTORY);
    }

    private String validateAndGetExtension(
            MultipartFile file,
            String requiredExtension
    ) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException(
                    "Nelze uložit prázdný soubor."
            );
        }

        String originalFileName = StringUtils.cleanPath(
                Objects.requireNonNullElse(
                        file.getOriginalFilename(),
                        ""
                )
        );

        if (!StringUtils.hasText(originalFileName)) {
            throw new IllegalArgumentException(
                    "Soubor nemá platný název."
            );
        }

        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException(
                    "Název souboru obsahuje nepovolenou cestu: "
                            + originalFileName
            );
        }

        String extension = StringUtils
                .getFilenameExtension(originalFileName);

        if (!StringUtils.hasText(extension)) {
            throw new IllegalArgumentException(
                    "Soubor musí obsahovat příponu."
            );
        }

        String normalizedExtension = extension.toLowerCase(Locale.ROOT);
        if (requiredExtension != null && !requiredExtension.equals(normalizedExtension)) {
            throw new IllegalArgumentException(
                    "Soubor musí mít příponu ." + requiredExtension + "."
            );
        }

        return normalizedExtension;
    }

    private String storeWithGeneratedName(
            MultipartFile file,
            String extension,
            String relativeDirectory
    ) {
        String generatedFileName = UUID.randomUUID() + "." + extension;
        String storedFileName = relativeDirectory == null
                ? generatedFileName
                : relativeDirectory + "/" + generatedFileName;

        Path targetLocation = resolveSafePath(
                storedFileName
        );

        try {
            Files.createDirectories(targetLocation.getParent());
            Files.copy(
                    file.getInputStream(),
                    targetLocation,
                    StandardCopyOption.REPLACE_EXISTING
            );

            log.info(
                    "Stored image locally: {}",
                    storedFileName
            );

            return storedFileName;
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Soubor se nepodařilo uložit.",
                    exception
            );
        }
    }

    @Override
    public Resource loadFileAsResource(
            String fileName
    ) {
        Path filePath = resolveSafePath(fileName);

        try {
            Resource resource = new UrlResource(
                    filePath.toUri()
            );

            if (resource.exists()
                    && resource.isReadable()) {
                return resource;
            }

            throw new FileNotFoundException(
                    "Soubor nebyl nalezen: "
                            + fileName
            );
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Soubor nebyl nalezen: "
                            + fileName,
                    exception
            );
        }
    }

    @Override
    public void deleteFile(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            return;
        }

        Path filePath = resolveSafePath(fileName);

        try {
            boolean deleted =
                    Files.deleteIfExists(filePath);

            if (deleted) {
                log.info(
                        "Deleted image file: {}",
                        fileName
                );
            } else {
                log.warn(
                        "Image file did not exist during deletion: {}",
                        fileName
                );
            }
        } catch (IOException exception) {
            throw new IllegalStateException(
                    "Soubor "
                            + fileName
                            + " se nepodařilo smazat.",
                    exception
            );
        }
    }

    private Path resolveSafePath(String fileName) {
        if (!StringUtils.hasText(fileName)) {
            throw new IllegalArgumentException(
                    "Název souboru nesmí být prázdný."
            );
        }

        Path resolvedPath = fileStorageLocation
                .resolve(fileName)
                .normalize();

        if (!resolvedPath.startsWith(
                fileStorageLocation
        )) {
            throw new IllegalArgumentException(
                    "Neplatná cesta k souboru: "
                            + fileName
            );
        }

        return resolvedPath;
    }
}
