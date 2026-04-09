package com.vbforge.educationapi.controller;

import com.vbforge.educationapi.domain.Enrollment;
import com.vbforge.educationapi.domain.EnrollmentStatus;
import com.vbforge.educationapi.domain.Submission;
import com.vbforge.educationapi.domain.SubmissionStatus;
import com.vbforge.educationapi.repository.CourseRepository;
import com.vbforge.educationapi.repository.EnrollmentRepository;
import com.vbforge.educationapi.repository.SubmissionRepository;
import com.vbforge.educationapi.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final CourseRepository courseRepository;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final StudentService studentService;

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        // Get student by email
        var student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
        Long studentId = student.getId();
        
        // Get all enrollments for this student
        List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
        
        // Calculate stats
        long activeCourses = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                .count();
        
        long completedCourses = enrollments.stream()
                .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                .count();
        
        // Get all submissions for this student
        List<Submission> submissions = submissionRepository.findByStudentId(studentId);
        long completedAssignments = submissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.GRADED)
                .count();
        
        // Calculate average grade
        BigDecimal averageGrade = submissions.stream()
                .filter(s -> s.getScore() != null)
                .map(Submission::getScore)
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(
                        submissions.stream().filter(s -> s.getScore() != null).count() > 0 
                                ? submissions.stream().filter(s -> s.getScore() != null).count() 
                                : 1), 
                        2, RoundingMode.HALF_UP);
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("activeCourses", activeCourses);
        stats.put("completedCourses", completedCourses);
        stats.put("completedAssignments", completedAssignments);
        stats.put("averageGrade", averageGrade);
        
        // Prepare course list with progress
        var courses = enrollments.stream().map(enrollment -> {
            Map<String, Object> courseMap = new HashMap<>();
            courseMap.put("id", enrollment.getCourse().getId());
            courseMap.put("name", enrollment.getCourse().getName());
            courseMap.put("instructor", enrollment.getCourse().getInstructor());
            courseMap.put("progressPct", enrollment.getProgressPct());
            return courseMap;
        }).collect(Collectors.toList());
        
        // Get upcoming assignments (simplified - you can enhance this)
        var upcomingAssignments = submissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.PENDING)
                .limit(5)
                .map(submission -> {
                    Map<String, Object> assignmentMap = new HashMap<>();
                    assignmentMap.put("id", submission.getAssignment().getId());
                    assignmentMap.put("title", submission.getAssignment().getTitle());
                    assignmentMap.put("courseName", submission.getAssignment().getModule().getCourse().getName());
                    assignmentMap.put("dueDate", submission.getAssignment().getDueDate());
                    assignmentMap.put("status", submission.getStatus().name());
                    assignmentMap.put("score", submission.getScore());
                    return assignmentMap;
                })
                .collect(Collectors.toList());
        
        model.addAttribute("stats", stats);
        model.addAttribute("courses", courses);
        model.addAttribute("upcomingAssignments", upcomingAssignments);
        model.addAttribute("title", "Dashboard");
        
        return "dashboard";
    }
}