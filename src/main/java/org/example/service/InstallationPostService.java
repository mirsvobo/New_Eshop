package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.model.InstallationImage;
import org.example.model.InstallationPost;
import org.example.repository.InstallationPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class InstallationPostService {

    private static final String MODULE_NAME =
            "MONTÁŽE";

    private static final int MAX_IMAGES_PER_POST =
            12;

    private static final long MAX_IMAGE_SIZE_BYTES =
            10L * 1024L * 1024L;

    private static final Set<String>
            ALLOWED_CONTENT_TYPES =
            Set.of(
                    "image/jpeg",
                    "image/png",
                    "image/webp"
            );

    private final InstallationPostRepository
            installationPostRepository;

    private final FileStorageService
            fileStorageService;

    private final AuditService
            auditService;

    @Transactional
    public InstallationPost savePostWithImages(
            InstallationPost submittedPost,
            List<MultipartFile> imageFiles
    ) {
        Objects.requireNonNull(
                submittedPost,
                "Příspěvek nesmí být null."
        );

        boolean newPost =
                submittedPost.getId() == null;

        InstallationPost postToSave;

        if (newPost) {
            postToSave = submittedPost;

            ensureImageCollectionExists(
                    postToSave
            );
        } else {
            postToSave = findPostById(
                    submittedPost.getId()
            );

            ensureImageCollectionExists(
                    postToSave
            );
        }

        List<MultipartFile> nonEmptyFiles =
                getNonEmptyFiles(imageFiles);

        /*
         * Celá dávka se ověří před uložením
         * prvního fyzického souboru.
         */
        validateImageUploads(
                postToSave,
                nonEmptyFiles
        );

        /*
         * Existující entitu aktualizujeme až po
         * úspěšné validaci všech souborů.
         */
        if (!newPost) {
            updatePostFields(
                    postToSave,
                    submittedPost
            );
        }

        addNewImages(
                postToSave,
                nonEmptyFiles
        );

        InstallationPost savedPost =
                installationPostRepository.save(
                        postToSave
                );

        if (newPost) {
            auditService.log(
                    MODULE_NAME,
                    "VYTVOŘENÍ",
                    "Vytvořen příspěvek z montáže: "
                            + postToSave.getTitle()
                            + "."
            );
        } else {
            auditService.log(
                    MODULE_NAME,
                    "ÚPRAVA",
                    "Upraven příspěvek z montáže: "
                            + postToSave.getTitle()
                            + "."
            );
        }

        return savedPost;
    }

    @Transactional
    public InstallationPost toggleActive(
            Long postId
    ) {
        InstallationPost post =
                findPostById(postId);

        post.setActive(
                !post.isActive()
        );

        InstallationPost savedPost =
                installationPostRepository.save(
                        post
                );

        if (post.isActive()) {
            auditService.log(
                    MODULE_NAME,
                    "ZVEŘEJNĚNÍ",
                    "Příspěvek z montáže '"
                            + post.getTitle()
                            + "' byl zveřejněn."
            );
        } else {
            auditService.log(
                    MODULE_NAME,
                    "SKRYTÍ",
                    "Příspěvek z montáže '"
                            + post.getTitle()
                            + "' byl skryt."
            );
        }

        return savedPost;
    }

    @Transactional
    public void deletePost(
            Long postId
    ) {
        InstallationPost post =
                findPostById(postId);

        String postTitle =
                post.getTitle();

        int imageCount =
                post.getImages() == null
                        ? 0
                        : post.getImages().size();

        List<String> imageFileNames =
                post.getImages() == null
                        ? List.of()
                        : post.getImages()
                        .stream()
                        .map(
                                InstallationImage
                                        ::getImageUrl
                        )
                        .filter(
                                Objects::nonNull
                        )
                        .filter(
                                fileName ->
                                        !fileName.isBlank()
                        )
                        .toList();

        /*
         * Nejdříve odstraníme databázovou entitu
         * a vynutíme provedení SQL. Fyzické soubory
         * smažeme až poté.
         */
        installationPostRepository.delete(post);
        installationPostRepository.flush();

        for (String imageFileName
                : imageFileNames) {

            fileStorageService.deleteFile(
                    imageFileName
            );
        }

        auditService.log(
                MODULE_NAME,
                "SMAZÁNÍ",
                "Smazán příspěvek z montáže: "
                        + postTitle
                        + " (fotografií: "
                        + imageCount
                        + ")."
        );
    }

    @Transactional
    public void deleteImage(
            Long postId,
            Long imageId
    ) {
        InstallationPost post =
                findPostById(postId);

        if (post.getImages() == null) {
            throw imageNotFoundException(
                    imageId
            );
        }

        InstallationImage imageToDelete =
                post.getImages()
                        .stream()
                        .filter(image ->
                                Objects.equals(
                                        image.getId(),
                                        imageId
                                )
                        )
                        .findFirst()
                        .orElseThrow(() ->
                                imageNotFoundException(
                                        imageId
                                )
                        );

        String postTitle =
                post.getTitle();

        String imageFileName =
                imageToDelete.getImageUrl();

        post.removeImage(
                imageToDelete
        );

        resequenceImages(post);

        installationPostRepository.save(post);
        installationPostRepository.flush();

        if (StringUtils.hasText(
                imageFileName
        )) {
            fileStorageService.deleteFile(
                    imageFileName
            );
        }

        auditService.log(
                MODULE_NAME,
                "SMAZÁNÍ FOTOGRAFIE",
                "Z příspěvku z montáže '"
                        + postTitle
                        + "' byla smazána fotografie: "
                        + imageFileName
                        + "."
        );
    }

    private void validateImageUploads(
            InstallationPost post,
            List<MultipartFile> imageFiles
    ) {
        validateImageCount(
                post,
                imageFiles.size()
        );

        for (MultipartFile file : imageFiles) {
            validateSingleImage(file);
        }
    }

    private void validateImageCount(
            InstallationPost post,
            int newImageCount
    ) {
        int existingImageCount =
                post.getImages() == null
                        ? 0
                        : post.getImages().size();

        int resultingImageCount =
                existingImageCount
                        + newImageCount;

        if (resultingImageCount
                > MAX_IMAGES_PER_POST) {

            throw new IllegalArgumentException(
                    "Jeden příspěvek může obsahovat "
                            + "nejvýše "
                            + MAX_IMAGES_PER_POST
                            + " fotografií. "
                            + "Aktuálně obsahuje "
                            + existingImageCount
                            + " a pokoušíte se přidat "
                            + newImageCount
                            + "."
            );
        }
    }

    private void validateSingleImage(
            MultipartFile file
    ) {
        String originalFileName =
                StringUtils.cleanPath(
                        Objects.requireNonNullElse(
                                file.getOriginalFilename(),
                                ""
                        )
                );

        if (!StringUtils.hasText(
                originalFileName
        )) {
            throw new IllegalArgumentException(
                    "Nahrávaný soubor nemá platný název."
            );
        }

        if (originalFileName.contains("..")) {
            throw new IllegalArgumentException(
                    "Název souboru obsahuje "
                            + "nepovolenou cestu: "
                            + originalFileName
            );
        }

        if (file.getSize()
                > MAX_IMAGE_SIZE_BYTES) {

            throw new IllegalArgumentException(
                    "Fotografie "
                            + originalFileName
                            + " je příliš velká. "
                            + "Maximální povolená "
                            + "velikost je 10 MB."
            );
        }

        String contentType =
                normalizeContentType(
                        file.getContentType()
                );

        if (!ALLOWED_CONTENT_TYPES.contains(
                contentType
        )) {
            throw new IllegalArgumentException(
                    "Soubor "
                            + originalFileName
                            + " nemá podporovaný typ. "
                            + "Povolené jsou pouze "
                            + "JPG, PNG nebo WEBP."
            );
        }

        String extension =
                normalizeExtension(
                        StringUtils
                                .getFilenameExtension(
                                        originalFileName
                                )
                );

        if (!isSupportedExtension(
                extension
        )) {
            throw new IllegalArgumentException(
                    "Soubor "
                            + originalFileName
                            + " nemá podporovanou příponu. "
                            + "Povolené jsou pouze "
                            + "JPG, PNG nebo WEBP."
            );
        }

        if (!contentTypeMatchesExtension(
                contentType,
                extension
        )) {
            throw new IllegalArgumentException(
                    "Typ souboru "
                            + originalFileName
                            + " neodpovídá jeho příponě."
            );
        }
    }

    private List<MultipartFile> getNonEmptyFiles(
            List<MultipartFile> imageFiles
    ) {
        if (imageFiles == null
                || imageFiles.isEmpty()) {
            return List.of();
        }

        return imageFiles
                .stream()
                .filter(Objects::nonNull)
                .filter(file -> !file.isEmpty())
                .toList();
    }

    private String normalizeContentType(
            String contentType
    ) {
        if (contentType == null) {
            return "";
        }

        return contentType
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private String normalizeExtension(
            String extension
    ) {
        if (extension == null) {
            return "";
        }

        return extension
                .trim()
                .toLowerCase(
                        Locale.ROOT
                );
    }

    private boolean isSupportedExtension(
            String extension
    ) {
        return extension.equals("jpg")
                || extension.equals("jpeg")
                || extension.equals("png")
                || extension.equals("webp");
    }

    private boolean contentTypeMatchesExtension(
            String contentType,
            String extension
    ) {
        return switch (contentType) {
            case "image/jpeg" ->
                    extension.equals("jpg")
                            || extension.equals(
                            "jpeg"
                    );

            case "image/png" ->
                    extension.equals("png");

            case "image/webp" ->
                    extension.equals("webp");

            default -> false;
        };
    }

    private InstallationPost findPostById(
            Long postId
    ) {
        return installationPostRepository
                .findById(postId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Příspěvek s ID "
                                        + postId
                                        + " nebyl nalezen."
                        )
                );
    }

    private IllegalArgumentException
    imageNotFoundException(
            Long imageId
    ) {
        return new IllegalArgumentException(
                "Obrázek s ID "
                        + imageId
                        + " nebyl v příspěvku nalezen."
        );
    }

    private void ensureImageCollectionExists(
            InstallationPost post
    ) {
        if (post.getImages() == null) {
            post.setImages(
                    new ArrayList<>()
            );
        }
    }

    private void updatePostFields(
            InstallationPost existingPost,
            InstallationPost submittedPost
    ) {
        existingPost.setTitle(
                submittedPost.getTitle()
        );

        existingPost.setProductName(
                submittedPost.getProductName()
        );

        existingPost.setAssemblyDate(
                submittedPost.getAssemblyDate()
        );

        existingPost.setContent(
                submittedPost.getContent()
        );

        existingPost.setActive(
                submittedPost.isActive()
        );
    }

    private void addNewImages(
            InstallationPost post,
            List<MultipartFile> imageFiles
    ) {
        if (imageFiles.isEmpty()) {
            return;
        }

        int nextDisplayOrder =
                determineNextDisplayOrder(
                        post
                );

        for (MultipartFile file : imageFiles) {
            String storedFileName =
                    fileStorageService.storeFile(
                            file
                    );

            InstallationImage image =
                    InstallationImage.builder()
                            .imageUrl(
                                    storedFileName
                            )
                            .displayOrder(
                                    nextDisplayOrder++
                            )
                            .build();

            post.addImage(image);
        }
    }

    private int determineNextDisplayOrder(
            InstallationPost post
    ) {
        if (post.getImages() == null
                || post.getImages().isEmpty()) {
            return 0;
        }

        return post.getImages()
                .stream()
                .map(
                        InstallationImage
                                ::getDisplayOrder
                )
                .filter(Objects::nonNull)
                .mapToInt(
                        Integer::intValue
                )
                .max()
                .orElse(-1)
                + 1;
    }

    private void resequenceImages(
            InstallationPost post
    ) {
        if (post.getImages() == null) {
            return;
        }

        for (int index = 0;
             index < post.getImages().size();
             index++) {

            post.getImages()
                    .get(index)
                    .setDisplayOrder(
                            index
                    );
        }
    }
}