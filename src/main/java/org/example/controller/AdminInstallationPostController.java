package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.model.InstallationPost;
import org.example.repository.InstallationPostRepository;
import org.example.service.InstallationPostService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/admin/installation-posts")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
public class AdminInstallationPostController {

    private final InstallationPostService installationPostService;
    private final InstallationPostRepository installationPostRepository;

    @GetMapping("")
    public String listPosts(Model model) {
        model.addAttribute("posts", installationPostRepository.findAll());
        return "admin/installation-post-list";
    }

    @GetMapping("/create")
    public String showCreateForm(Model model) {
        model.addAttribute("installationPost", new InstallationPost());
        return "admin/installation-post-form";
    }

    @PostMapping("/save")
    public String savePost(@ModelAttribute InstallationPost installationPost,
                           @RequestParam(value = "imageFiles", required = false) List<MultipartFile> imageFiles,
                           RedirectAttributes ra) {
        installationPostService.savePostWithImages(installationPost, imageFiles);
        ra.addFlashAttribute("success", "Příspěvek byl úspěšně uložen.");
        return "redirect:/admin/installation-posts";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        InstallationPost post = installationPostRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Příspěvek nenalezen"));
        model.addAttribute("installationPost", post);
        return "admin/installation-post-form";
    }

    @PostMapping("/delete/{id}")
    public String deletePost(@PathVariable Long id, RedirectAttributes ra) {
        installationPostRepository.deleteById(id);
        ra.addFlashAttribute("success", "Příspěvek byl smazán.");
        return "redirect:/admin/installation-posts";
    }
}