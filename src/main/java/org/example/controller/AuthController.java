package org.example.controller;

import org.example.model.User;
import org.example.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String loginPage() {
        return "login";
    }

    @GetMapping("/registrace")
    public String registerForm(Model model) {
        model.addAttribute("user", new User());
        return "register";
    }

    @PostMapping("/registrace")
    public String processRegistration(@ModelAttribute User user, RedirectAttributes ra) {
        if (userRepository.findByEmail(user.getEmail()).isPresent()) {
            ra.addFlashAttribute("error", "Uživatel s tímto emailem již existuje.");
            return "redirect:/registrace";
        }
        user.setRole(User.Role.ROLE_CUSTOMER);
        user.setPassword(passwordEncoder.encode(user.getPassword()));
        userRepository.save(user);

        ra.addFlashAttribute("success", "Registrace proběhla úspěšně. Nyní se můžete přihlásit.");
        return "redirect:/login";
    }
}