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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/admin/installation-posts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminInstallationPostController {

    private final InstallationPostService installationPostService;
    private final InstallationPostRepository installationPostRepository;

    @GetMapping
    public String listPosts(Model model) {
        List<InstallationPost> posts = installationPostRepository.findAll()
                .stream()
                .sorted(Comparator.comparing(
                        InstallationPost::getAssemblyDate,
                        Comparator.nullsLast(Comparator.reverseOrder())
                ))
                .toList();

        model.addAttribute("posts", posts);
        return "admin/installation-post-list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        InstallationPost installationPost = new InstallationPost();
        installationPost.setActive(true);

        model.addAttribute("installationPost", installationPost);
        return "admin/installation-post-form.html";
    }

    @PostMapping("/save")
    public String savePost(
            @Valid @ModelAttribute("installationPost")
            InstallationPost installationPost,
            BindingResult bindingResult,
            @RequestParam(
                    value = "imageFiles",
                    required = false
            )
            List<MultipartFile> imageFiles,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            return "admin/installation-post-form.html";
        }

        installationPostService.savePostWithImages(
                installationPost,
                imageFiles
        );

        redirectAttributes.addFlashAttribute(
                "success",
                "Příspěvek byl úspěšně uložen."
        );

        return "redirect:/admin/installation-posts";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(
            @PathVariable Long id,
            Model model
    ) {
        InstallationPost post = installationPostRepository
                .findById(id)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Příspěvek nenalezen"
                        )
                );

        model.addAttribute("installationPost", post);
        return "admin/installation-post-form.html";
    }

    @PostMapping("/delete/{id}")
    public String deletePost(
            @PathVariable Long id,
            RedirectAttributes redirectAttributes
    ) {
        installationPostRepository.deleteById(id);

        redirectAttributes.addFlashAttribute(
                "success",
                "Příspěvek byl smazán."
        );

        return "redirect:/admin/installation-posts";
    }
}