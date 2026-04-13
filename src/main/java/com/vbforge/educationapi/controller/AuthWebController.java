package com.vbforge.educationapi.controller;

import com.vbforge.educationapi.dto.student.StudentRequestDto;
import com.vbforge.educationapi.exception.DuplicateResourceException;
import com.vbforge.educationapi.repository.StudentRepository;
import com.vbforge.educationapi.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class AuthWebController {

    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/register")
    public String showRegisterForm(Model model) {
        model.addAttribute("title", "Register");
        return "register";
    }

    @PostMapping("/register")
    public String register(@RequestParam String name,
                           @RequestParam String email,
                           @RequestParam String password,
                           @RequestParam String confirmPassword,
                           RedirectAttributes redirectAttributes) {

        if (!password.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            return "redirect:/register";
        }

        try {
            if (studentRepository.existsByEmail(email)) {
                redirectAttributes.addFlashAttribute("error", "Email already registered!");
                return "redirect:/register";
            }

            StudentRequestDto dto = new StudentRequestDto();
            dto.setName(name);
            dto.setEmail(email);
            dto.setPassword(password);

            studentService.register(dto);
            redirectAttributes.addFlashAttribute("message", "Registration successful! Please login.");
            return "redirect:/login?registered=true";

        } catch (DuplicateResourceException e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
            return "redirect:/register";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Registration failed: " + e.getMessage());
            return "redirect:/register";
        }
    }


    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isInstructor = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR"));

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        } else if (isInstructor) {
            return "redirect:/instructor/dashboard";
        } else {
            return "redirect:/student/dashboard";
        }
    }

    @GetMapping("/dashboard")
    public String redirectOldDashboard(Authentication authentication) {
        if (authentication == null) {
            return "redirect:/login";
        }

        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isInstructor = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR"));

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        } else if (isInstructor) {
            return "redirect:/instructor/dashboard";
        } else {
            return "redirect:/student/dashboard";
        }
    }
}