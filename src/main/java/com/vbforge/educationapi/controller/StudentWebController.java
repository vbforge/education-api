package com.vbforge.educationapi.controller;

import com.vbforge.educationapi.domain.*;
import com.vbforge.educationapi.dto.assignment.AssignmentResponseDto;
import com.vbforge.educationapi.dto.common.PageResponseDto;
import com.vbforge.educationapi.dto.course.CourseResponseDto;
import com.vbforge.educationapi.dto.enrollment.EnrollmentRequestDto;
import com.vbforge.educationapi.dto.module.ModuleResponseDto;
import com.vbforge.educationapi.repository.*;
import com.vbforge.educationapi.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
public class StudentWebController {

    private final StudentService studentService;
    private final CourseService courseService;
    private final ModuleService moduleService;
    private final AssignmentService assignmentService;
    private final EnrollmentService enrollmentService;
    private final SubmissionService submissionService;
    private final StorageService storageService;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final AnnouncementRepository announcementRepository;

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            Long studentId = student.getId();
            
            List<Enrollment> enrollments = enrollmentRepository.findByStudentId(studentId);
            
            long activeCourses = enrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                    .count();
            long completedCourses = enrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                    .count();
            
            List<Submission> submissions = submissionRepository.findByStudentId(studentId);
            long completedAssignments = submissions.stream()
                    .filter(s -> s.getStatus() == SubmissionStatus.GRADED)
                    .count();
            int totalAssignments = submissions.size();
            
            double avgGrade = submissions.stream()
                    .filter(s -> s.getScore() != null)
                    .mapToDouble(s -> s.getScore().doubleValue())
                    .average()
                    .orElse(0.0);
            
            List<Map<String, Object>> courses = new ArrayList<>();
            for (Enrollment enrollment : enrollments) {
                if (enrollment.getStatus() != EnrollmentStatus.DROPPED) {
                    Map<String, Object> courseMap = new HashMap<>();
                    courseMap.put("id", enrollment.getCourse().getId());
                    courseMap.put("name", enrollment.getCourse().getName());
                    courseMap.put("instructor", enrollment.getCourse().getInstructor());
                    courseMap.put("progressPct", enrollment.getProgressPct());
                    courses.add(courseMap);
                }
            }
            
            model.addAttribute("studentName", student.getName());
            model.addAttribute("activeCourses", activeCourses);
            model.addAttribute("completedCourses", completedCourses);
            model.addAttribute("completedAssignments", completedAssignments);
            model.addAttribute("totalAssignments", totalAssignments);
            model.addAttribute("averageGrade", Math.round(avgGrade * 10) / 10.0);
            model.addAttribute("courses", courses);
            model.addAttribute("title", "Student Dashboard");
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }
        
        return "student-dashboard";
    }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "9") int size,
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        if (userDetails == null) return "redirect:/login";
        
        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            Set<Long> enrolledCourseIds = enrollmentRepository.findByStudentId(student.getId())
                    .stream().map(e -> e.getCourse().getId()).collect(Collectors.toSet());
            
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            PageResponseDto<CourseResponseDto> coursePage;
            
            if (keyword != null && !keyword.isBlank()) {
                coursePage = courseService.search(keyword, pageable);
                model.addAttribute("keyword", keyword);
            } else {
                coursePage = courseService.findAll(pageable);
            }
            
            List<Map<String, Object>> enhancedCourses = new ArrayList<>();
            for (CourseResponseDto course : coursePage.getContent()) {
                Map<String, Object> enhanced = new HashMap<>();
                enhanced.put("id", course.getId());
                enhanced.put("name", course.getName());
                enhanced.put("description", course.getDescription());
                enhanced.put("instructor", course.getInstructor());
                enhanced.put("moduleCount", course.getModuleCount());
                enhanced.put("enrollmentCount", course.getEnrollmentCount());
                enhanced.put("enrolled", enrolledCourseIds.contains(course.getId()));
                enhancedCourses.add(enhanced);
            }
            
            model.addAttribute("courses", enhancedCourses);
            model.addAttribute("page", coursePage);
            model.addAttribute("title", "Courses");
            
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("courses", new ArrayList<>());
        }
        
        return "student-courses";
    }

    @GetMapping("/courses/{id}")
    public String courseDetail(@PathVariable Long id,
                               @AuthenticationPrincipal UserDetails userDetails,
                               Model model) {
        if (userDetails == null) return "redirect:/login";
        
        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            CourseResponseDto course = courseService.findById(id);
            List<ModuleResponseDto> modules = moduleService.findByCourse(id);
            
            var enrollment = enrollmentRepository.findByStudentIdAndCourseId(student.getId(), id).orElse(null);
            BigDecimal progressPct = enrollment != null ? enrollment.getProgressPct() : BigDecimal.ZERO;
            BigDecimal grade = enrollment != null ? enrollment.getGrade() : null;
            
            model.addAttribute("course", course);
            model.addAttribute("modules", modules);
            model.addAttribute("moduleCount", modules.size());
            model.addAttribute("enrollmentCount", course.getEnrollmentCount());
            model.addAttribute("progressPct", progressPct);
            model.addAttribute("grade", grade);
            model.addAttribute("title", course.getName());
            
        } catch (Exception e) {
            model.addAttribute("error", "Course not found");
            return "redirect:/student/courses";
        }
        
        return "student-course-detail";
    }

    @PostMapping("/enroll")
    public String enroll(@RequestParam Long courseId,
                         @AuthenticationPrincipal UserDetails userDetails,
                         RedirectAttributes redirectAttributes) {
        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            EnrollmentRequestDto dto = new EnrollmentRequestDto();
            dto.setStudentId(student.getId());
            dto.setCourseId(courseId);
            enrollmentService.enroll(dto);
            redirectAttributes.addFlashAttribute("message", "Successfully enrolled!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/student/courses";
    }

    @GetMapping("/assignments/{id}/submit")
    public String showSubmitForm(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        if (userDetails == null) return "redirect:/login";
        
        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            AssignmentResponseDto assignment = assignmentService.findById(id);
            CourseResponseDto course = courseService.findById(assignment.getCourseId());
            
            var existingSubmission = submissionRepository
                    .findByAssignmentIdAndStudentId(id, student.getId()).orElse(null);
            
            model.addAttribute("assignment", assignment);
            model.addAttribute("courseId", assignment.getCourseId());
            model.addAttribute("courseName", course.getName());
            model.addAttribute("existingSubmission", existingSubmission);
            model.addAttribute("title", assignment.getTitle());
            
        } catch (Exception e) {
            model.addAttribute("error", "Assignment not found");
            return "redirect:/student/dashboard";
        }
        
        return "student-assignment-submit";
    }

    @PostMapping("/submit-assignment")
    public String submitAssignment(@RequestParam Long assignmentId,
                                   @RequestParam(required = false) MultipartFile file,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            String filePath = storageService.store(file, student.getId(), assignmentId);
            
            var submissionDto = new com.vbforge.educationapi.dto.submission.SubmissionRequestDto();
            submissionDto.setAssignmentId(assignmentId);
            submissionDto.setStudentId(student.getId());
            
            submissionService.submit(submissionDto, filePath);
            redirectAttributes.addFlashAttribute("message", "Assignment submitted successfully!");
            
            AssignmentResponseDto assignment = assignmentService.findById(assignmentId);
            return "redirect:/student/courses/" + assignment.getCourseId();
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Submission failed: " + e.getMessage());
            return "redirect:/student/assignments/" + assignmentId + "/submit";
        }
    }

    @GetMapping("/announcements")
    public String viewAnnouncements(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";

        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            List<Enrollment> enrollments = enrollmentRepository.findByStudentId(student.getId());

            List<Map<String, Object>> announcements = new ArrayList<>();
            for (Enrollment enrollment : enrollments) {
                List<Announcement> anns = announcementRepository.findByCourseIdOrderByPostedAtDesc(enrollment.getCourse().getId());
                for (Announcement ann : anns) {
                    Map<String, Object> annMap = new HashMap<>();
                    annMap.put("title", ann.getTitle());
                    annMap.put("message", ann.getMessage());
                    annMap.put("courseName", enrollment.getCourse().getName());
                    annMap.put("postedAt", ann.getPostedAt());
                    announcements.add(annMap);
                }
            }

            // Sort by posted date descending
            announcements.sort((a, b) -> {
                if (a.get("postedAt") == null) return 1;
                if (b.get("postedAt") == null) return -1;
                return ((LocalDateTime) b.get("postedAt")).compareTo((LocalDateTime) a.get("postedAt"));
            });

            model.addAttribute("announcements", announcements);
            model.addAttribute("title", "Announcements");

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "student-announcements";
    }

}