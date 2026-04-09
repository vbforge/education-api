package com.vbforge.educationapi.controller;

import com.vbforge.educationapi.domain.Enrollment;
import com.vbforge.educationapi.domain.EnrollmentStatus;
import com.vbforge.educationapi.domain.Submission;
import com.vbforge.educationapi.domain.SubmissionStatus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.vbforge.educationapi.domain.*;
import com.vbforge.educationapi.dto.assignment.AssignmentResponseDto;
import com.vbforge.educationapi.dto.common.PageResponseDto;
import com.vbforge.educationapi.dto.course.CourseResponseDto;
import com.vbforge.educationapi.dto.enrollment.EnrollmentRequestDto;
import com.vbforge.educationapi.dto.module.ModuleResponseDto;
import com.vbforge.educationapi.repository.EnrollmentRepository;
import com.vbforge.educationapi.repository.SubmissionRepository;
import com.vbforge.educationapi.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final StudentService studentService;
    private final CourseService courseService;
    private final ModuleService moduleService;
    private final AssignmentService assignmentService;
    private final EnrollmentService enrollmentService;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;


    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home() {
        return "redirect:/dashboard";
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        System.out.println("=== DASHBOARD DEBUG ===");

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            // Get student
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            Long studentId = student.getId();
            System.out.println("Student ID: " + studentId);

            // Get enrollments with course data loaded eagerly
            List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
            System.out.println("Enrollments found: " + enrollments.size());

            // Calculate stats - use the data we already have
            long activeCourses = enrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                    .count();
            long completedCourses = enrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                    .count();

            System.out.println("Active courses: " + activeCourses);

            // Get submissions
            List<Submission> submissions = submissionRepository.findByStudentId(studentId);
            long completedAssignments = submissions.stream()
                    .filter(s -> s.getStatus() == SubmissionStatus.GRADED)
                    .count();
            int totalAssignments = submissions.size();

            // Calculate average grade
            double avgGrade = submissions.stream()
                    .filter(s -> s.getScore() != null)
                    .mapToDouble(s -> s.getScore().doubleValue())
                    .average()
                    .orElse(0.0);

            // Build courses list - extract data while session is open
            List<Map<String, Object>> courses = new ArrayList<>();
            for (Enrollment enrollment : enrollments) {
                if (enrollment.getStatus() != EnrollmentStatus.DROPPED) {
                    Map<String, Object> courseMap = new HashMap<>();
                    // Access these properties while still in the same session
                    courseMap.put("id", enrollment.getCourse().getId());
                    courseMap.put("name", enrollment.getCourse().getName());
                    courseMap.put("instructor", enrollment.getCourse().getInstructor() != null ?
                            enrollment.getCourse().getInstructor() : "Staff");
                    courseMap.put("progressPct", enrollment.getProgressPct() != null ?
                            enrollment.getProgressPct() : 0);
                    courses.add(courseMap);
                    System.out.println("Added course: " + enrollment.getCourse().getName());
                }
            }

            // Build upcoming assignments
            List<Map<String, Object>> upcomingAssignments = new ArrayList<>();
            for (Submission submission : submissions) {
                if (submission.getStatus() == SubmissionStatus.PENDING &&
                        submission.getAssignment().getDueDate() != null) {
                    Map<String, Object> assignmentMap = new HashMap<>();
                    assignmentMap.put("id", submission.getAssignment().getId());
                    assignmentMap.put("title", submission.getAssignment().getTitle());
                    assignmentMap.put("courseName", submission.getAssignment().getModule().getCourse().getName());
                    assignmentMap.put("dueDate", submission.getAssignment().getDueDate());
                    assignmentMap.put("status", submission.getStatus().name());
                    assignmentMap.put("score", submission.getScore());
                    upcomingAssignments.add(assignmentMap);
                    System.out.println("Added assignment: " + submission.getAssignment().getTitle());
                }
            }

            // Set all attributes
            model.addAttribute("activeCourses", activeCourses);
            model.addAttribute("completedCourses", completedCourses);
            model.addAttribute("completedAssignments", completedAssignments);
            model.addAttribute("totalAssignments", totalAssignments);
            model.addAttribute("averageGrade", Math.round(avgGrade * 10) / 10.0);
            model.addAttribute("courses", courses);
            model.addAttribute("upcomingAssignments", upcomingAssignments);

            System.out.println("Courses added to model: " + courses.size());

        } catch (Exception e) {
            System.err.println("ERROR in dashboard: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("activeCourses", 0);
            model.addAttribute("completedCourses", 0);
            model.addAttribute("completedAssignments", 0);
            model.addAttribute("totalAssignments", 0);
            model.addAttribute("averageGrade", 0);
            model.addAttribute("courses", new ArrayList<>());
            model.addAttribute("upcomingAssignments", new ArrayList<>());
        }

        return "dashboard";
    }

    @GetMapping("/courses")
    public String courses(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "9") int size,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {

        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            // Get current student
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            Long studentId = student.getId();

            // Get enrolled course IDs
            Set<Long> enrolledCourseIds = enrollmentRepository.findByStudentId(studentId)
                    .stream()
                    .map(e -> e.getCourse().getId())
                    .collect(Collectors.toSet());

            // Get courses with pagination
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            PageResponseDto<CourseResponseDto> coursePage;

            if (keyword != null && !keyword.isBlank()) {
                coursePage = courseService.search(keyword, pageable);
                model.addAttribute("keyword", keyword);
            } else {
                coursePage = courseService.findAll(pageable);
            }

            // Enhance courses with enrollment status
            List<Map<String, Object>> enhancedCourses = new ArrayList<>();
            for (CourseResponseDto course : coursePage.getContent()) {
                Map<String, Object> enhanced = new HashMap<>();
                enhanced.put("id", course.getId());
                enhanced.put("name", course.getName());
                enhanced.put("description", course.getDescription() != null ? course.getDescription() : "");
                enhanced.put("instructor", course.getInstructor() != null ? course.getInstructor() : "Staff");
                enhanced.put("moduleCount", course.getModuleCount());
                enhanced.put("enrollmentCount", course.getEnrollmentCount());

                boolean isEnrolled = enrolledCourseIds.contains(course.getId());
                enhanced.put("enrolled", isEnrolled);

                if (isEnrolled) {
                    enrollmentRepository.findByStudentIdAndCourseId(studentId, course.getId())
                            .ifPresent(e -> enhanced.put("progressPct", e.getProgressPct()));
                } else {
                    enhanced.put("progressPct", 0);
                }
                enhancedCourses.add(enhanced);
            }

            model.addAttribute("courses", enhancedCourses);
            model.addAttribute("page", coursePage);
            model.addAttribute("title", "Courses");

        } catch (Exception e) {
            model.addAttribute("error", "Failed to load courses: " + e.getMessage());
            model.addAttribute("courses", new ArrayList<>());
        }

        return "courses";
    }

    @PostMapping("/enroll")
    public String enroll(@RequestParam Long courseId,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());

            EnrollmentRequestDto enrollDto = new EnrollmentRequestDto();
            enrollDto.setStudentId(student.getId());
            enrollDto.setCourseId(courseId);

            enrollmentService.enroll(enrollDto);
            redirectAttributes.addFlashAttribute("message", "Successfully enrolled in course!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }

        return "redirect:/courses";
    }

    @GetMapping("/courses/{id}")
    public String courseDetail(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            Long studentId = student.getId();

            // Get course details
            CourseResponseDto course = courseService.findById(id);

            // Get modules with assignments
            List<ModuleResponseDto> modules = moduleService.findByCourse(id);

            // Get submissions for this student to determine assignment status
            List<Submission> submissions = submissionRepository.findByStudentId(studentId);
            Map<Long, Submission> submissionMap = submissions.stream()
                    .collect(Collectors.toMap(s -> s.getAssignment().getId(), s -> s));

            // Build enhanced modules with assignments
            List<Map<String, Object>> enhancedModules = new ArrayList<>();
            for (ModuleResponseDto module : modules) {
                Map<String, Object> enhancedModule = new HashMap<>();
                enhancedModule.put("id", module.getId());
                enhancedModule.put("title", module.getTitle());
                enhancedModule.put("orderIndex", module.getOrderIndex());

                // Get assignments for this module
                List<AssignmentResponseDto> assignments = assignmentService.findByModule(module.getId());
                List<Map<String, Object>> enhancedAssignments = new ArrayList<>();

                for (AssignmentResponseDto assignment : assignments) {
                    Map<String, Object> enhancedAssignment = new HashMap<>();
                    enhancedAssignment.put("id", assignment.getId());
                    enhancedAssignment.put("title", assignment.getTitle());
                    enhancedAssignment.put("dueDate", assignment.getDueDate());
                    enhancedAssignment.put("pointsPossible", assignment.getPointsPossible());
                    enhancedAssignment.put("description", assignment.getDescription());

                    Submission submission = submissionMap.get(assignment.getId());
                    if (submission != null) {
                        enhancedAssignment.put("status", submission.getStatus().name());
                        enhancedAssignment.put("score", submission.getScore());
                    } else {
                        enhancedAssignment.put("status", "PENDING");
                        enhancedAssignment.put("score", null);
                    }
                    enhancedAssignments.add(enhancedAssignment);
                }
                enhancedModule.put("assignments", enhancedAssignments);
                enhancedModules.add(enhancedModule);
            }

            // Get enrollment for progress
            BigDecimal progressPct = BigDecimal.ZERO;
            BigDecimal grade = null;
            var enrollmentOpt = enrollmentRepository.findByStudentIdAndCourseId(studentId, id);
            if (enrollmentOpt.isPresent()) {
                progressPct = enrollmentOpt.get().getProgressPct();
                grade = enrollmentOpt.get().getGrade();
            }

            model.addAttribute("course", course);
            model.addAttribute("modules", enhancedModules);
            model.addAttribute("moduleCount", modules.size());
            model.addAttribute("enrollmentCount", course.getEnrollmentCount());
            model.addAttribute("progressPct", progressPct);
            model.addAttribute("grade", grade);
            model.addAttribute("title", course.getName());

        } catch (Exception e) {
            System.err.println("Error in course detail: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Course not found: " + e.getMessage());
            return "redirect:/courses";
        }

        return "course-detail";
    }

}