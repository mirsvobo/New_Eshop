package org.example.service;

import lombok.RequiredArgsConstructor;
import org.example.model.InstallationImage;
import org.example.model.InstallationPost;
import org.example.repository.InstallationPostRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Service
@RequiredArgsConstructor
public class InstallationPostService {

    private final InstallationPostRepository installationPostRepository;
    private final FileStorageService fileStorageService;

    @Transactional
    public InstallationPost savePostWithImages(InstallationPost post, List<MultipartFile> imageFiles) {
        if (imageFiles != null) {
            int displayOrder = 0;
            for (MultipartFile file : imageFiles) {
                if (!file.isEmpty()) {
                    String fileName = fileStorageService.storeFile(file);

                    InstallationImage image = InstallationImage.builder()
                            .imageUrl(fileName)
                            .displayOrder(displayOrder++)
                            .build();

                    post.addImage(image);
                }
            }
        }

        return installationPostRepository.save(post);
    }
}