package com.vbforge.educationapi.controller;

import com.vbforge.educationapi.domain.PasswordResetToken;
import com.vbforge.educationapi.domain.Student;
import com.vbforge.educationapi.dto.student.StudentRequestDto;
import com.vbforge.educationapi.exception.DuplicateResourceException;
import com.vbforge.educationapi.repository.PasswordResetTokenRepository;
import com.vbforge.educationapi.repository.StudentRepository;
import com.vbforge.educationapi.service.NotificationService;
import com.vbforge.educationapi.service.StudentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Optional;
import java.util.UUID;

@Controller
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AuthWebController {

    private final StudentService studentService;
    private final StudentRepository studentRepository;
    private final PasswordEncoder passwordEncoder;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final NotificationService notificationService;

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

    @GetMapping("/forgot-password")
    public String showForgotPasswordForm() {
        System.out.println("=== FORGOT PASSWORD PAGE ACCESSED ===");
        return "forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email,
                                        RedirectAttributes redirectAttributes) {

        System.out.println("=== PROCESS FORGOT PASSWORD ===");
        System.out.println("Email: " + email);

        try {
            Optional<Student> studentOpt = studentRepository.findByEmail(email);

            if (studentOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "No account found with that email address.");
                return "redirect:/forgot-password";
            }

            Student student = studentOpt.get();

            // Delete any existing tokens for this student
            passwordResetTokenRepository.deleteByStudentId(student.getId());

            // Generate new token
            String token = UUID.randomUUID().toString();
            PasswordResetToken resetToken = PasswordResetToken.create(student, token);
            passwordResetTokenRepository.save(resetToken);

            // Send email with reset link
            String resetUrl = "http://localhost:8080/reset-password?token=" + token;
            String emailBody = String.format("""
            Hello %s,
            
            You requested to reset your password.
            
            Click the link below to reset your password (valid for 60 minutes):
            %s
            
            If you didn't request this, please ignore this email.
            
            — EduFlow Team
            """, student.getName(), resetUrl);

            notificationService.sendPasswordResetEmail(student.getEmail(), "Password Reset Request", emailBody);

            redirectAttributes.addFlashAttribute("message", "Password reset link sent to your email!");

        } catch (Exception e) {
            log.error("Error sending reset email: {}", e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Failed to send reset email. Please try again.");
        }

        return "redirect:/forgot-password";
    }

    // ✅ RESET PASSWORD - Show form
    @GetMapping("/reset-password")
    public String showResetPasswordForm(@RequestParam String token, Model model) {
        System.out.println("=== SHOW RESET PASSWORD FORM ===");
        System.out.println("Token: " + token);

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty()) {
            System.out.println("Token not found!");
            model.addAttribute("error", "Invalid or expired reset link. Please request a new one.");
            return "forgot-password";
        }

        if (tokenOpt.get().isExpired()) {
            System.out.println("Token expired!");
            model.addAttribute("error", "Invalid or expired reset link. Please request a new one.");
            return "forgot-password";
        }

        System.out.println("Token is valid");
        model.addAttribute("token", token);
        return "reset-password";
    }

    // ✅ RESET PASSWORD - Process form submission
    @PostMapping("/reset-password")
    public String processResetPassword(@RequestParam String token,
                                       @RequestParam String password,
                                       @RequestParam String confirmPassword,
                                       RedirectAttributes redirectAttributes) {
        System.out.println("=== PROCESS RESET PASSWORD ===");

        if (!password.equals(confirmPassword)) {
            System.out.println("Passwords do not match");
            redirectAttributes.addFlashAttribute("error", "Passwords do not match!");
            return "redirect:/reset-password?token=" + token;
        }

        Optional<PasswordResetToken> tokenOpt = passwordResetTokenRepository.findByToken(token);

        if (tokenOpt.isEmpty() || tokenOpt.get().isExpired()) {
            System.out.println("Token invalid or expired");
            redirectAttributes.addFlashAttribute("error", "Invalid or expired reset link.");
            return "redirect:/forgot-password";
        }

        PasswordResetToken resetToken = tokenOpt.get();
        Student student = resetToken.getStudent();

        System.out.println("Resetting password for student: " + student.getEmail());

        // Update password
        student.setPasswordHash(passwordEncoder.encode(password));
        studentRepository.save(student);

        // Delete used token
        passwordResetTokenRepository.delete(resetToken);

        System.out.println("Password reset successfully!");
        redirectAttributes.addFlashAttribute("message", "Password reset successfully! Please login with your new password.");
        return "redirect:/login";
    }

}