package org.example.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.model.InstallationPost;
import org.example.repository.InstallationPostRepository;
import org.example.service.InstallationPostService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin/installation-posts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminInstallationPostController {

    private static final String LIST_VIEW =
            "admin/installation-post-list";

    private static final String FORM_VIEW =
            "admin/installation-post-form";

    private static final String LIST_REDIRECT =
            "redirect:/admin/installation-posts";

    private final InstallationPostService
            installationPostService;

    private final InstallationPostRepository
            installationPostRepository;

    @GetMapping
    public String listPosts(Model model) {
        List<InstallationPost> posts =
                installationPostRepository
                        .findAll()
                        .stream()
                        .sorted(
                                Comparator.comparing(
                                        InstallationPost
                                                ::getAssemblyDate,
                                        Comparator.nullsLast(
                                                Comparator
                                                        .reverseOrder()
                                        )
                                )
                        )
                        .toList();

        model.addAttribute(
                "posts",
                posts
        );

        return LIST_VIEW;
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        InstallationPost installationPost =
                new InstallationPost();

        installationPost.setActive(true);

        model.addAttribute(
                "installationPost",
                installationPost
        );

        return FORM_VIEW;
    }

    @PostMapping("/save")
    public String savePost(
            @Valid
            @ModelAttribute("installationPost")
            InstallationPost installationPost,
            BindingResult bindingResult,
            @RequestParam(
                    value = "imageFiles",
                    required = false
            )
            List<MultipartFile> imageFiles,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            restoreExistingImages(
                    installationPost
            );

            return FORM_VIEW;
        }

        try {
            installationPostService
                    .savePostWithImages(
                            installationPost,
                            imageFiles
                    );
        } catch (IllegalArgumentException exception) {
            /*
             * Service odmítla neplatný upload.
             * Zachováme údaje odeslané formulářem
             * a při editaci znovu doplníme existující
             * obrázky z databáze.
             */
            restoreExistingImages(
                    installationPost
            );

            model.addAttribute(
                    "error",
                    exception.getMessage()
            );

            return FORM_VIEW;
        }

        redirectAttributes.addFlashAttribute(
                "success",
                "Příspěvek byl úspěšně uložen."
        );

        return LIST_REDIRECT;
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            Model model
    ) {
        InstallationPost post =
                findPostById(id);

        model.addAttribute(
                "installationPost",
                post
        );

        return FORM_VIEW;
    }

    @PostMapping("/delete/{id}")
    public String deletePost(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        installationPostService.deletePost(id);

        redirectAttributes.addFlashAttribute(
                "success",
                "Příspěvek a jeho fotografie byly smazány."
        );

        return LIST_REDIRECT;
    }

    @PostMapping(
            "/{postId}/images/{imageId}/delete"
    )
    public String deleteImage(
            @PathVariable Long postId,
            @PathVariable Long imageId,
            RedirectAttributes redirectAttributes
    ) {
        installationPostService.deleteImage(
                postId,
                imageId
        );

        redirectAttributes.addFlashAttribute(
                "success",
                "Fotografie byla odstraněna."
        );

        return "redirect:/admin/installation-posts/edit/"
                + postId;
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

    private void restoreExistingImages(
            InstallationPost submittedPost
    ) {
        if (submittedPost.getId() == null) {
            if (submittedPost.getImages() == null) {
                submittedPost.setImages(
                        new ArrayList<>()
                );
            }

            return;
        }

        installationPostRepository
                .findById(submittedPost.getId())
                .ifPresent(existingPost -> {
                    if (existingPost.getImages()
                            == null) {

                        submittedPost.setImages(
                                new ArrayList<>()
                        );

                        return;
                    }

                    submittedPost.setImages(
                            new ArrayList<>(
                                    existingPost
                                            .getImages()
                            )
                    );
                });
    }
}