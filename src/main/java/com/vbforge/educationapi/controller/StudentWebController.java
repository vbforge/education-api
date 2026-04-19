package com.vbforge.educationapi.controller;

import com.vbforge.educationapi.domain.*;
import com.vbforge.educationapi.dto.assignment.AssignmentResponseDto;
import com.vbforge.educationapi.dto.common.PageResponseDto;
import com.vbforge.educationapi.dto.course.CourseResponseDto;
import com.vbforge.educationapi.dto.enrollment.EnrollmentRequestDto;
import com.vbforge.educationapi.dto.module.ModuleResponseDto;
import com.vbforge.educationapi.dto.submission.SubmissionRequestDto;
import com.vbforge.educationapi.repository.*;
import com.vbforge.educationapi.service.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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


import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student")
@RequiredArgsConstructor
@Slf4j
public class StudentWebController {

    private final StudentService studentService;
    private final CourseService courseService;
    private final ModuleService moduleService;
    private final AssignmentService assignmentService;
    private final EnrollmentService enrollmentService;
    private final SubmissionService submissionService;
    private final StorageService storageService;
    private final ProgressService progressService;
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

            System.out.println("=== STUDENT DASHBOARD DEBUG ===");
            System.out.println("Student: " + student.getName() + " (ID: " + studentId + ")");

            // 1. Get enrollments with course data
            List<Object[]> enrollmentResults = enrollmentRepository.findEnrollmentsWithCourseDataNative(studentId);
            System.out.println("Enrollments found: " + enrollmentResults.size());

            long activeCourses = 0;
            long completedCourses = 0;
            List<Map<String, Object>> courses = new ArrayList<>();

            for (Object[] row : enrollmentResults) {
                String status = (String) row[4];
                if (!"DROPPED".equals(status)) {
                    if ("ACTIVE".equals(status)) activeCourses++;
                    if ("COMPLETED".equals(status)) completedCourses++;

                    Map<String, Object> courseMap = new HashMap<>();
                    courseMap.put("id", row[0]);
                    courseMap.put("name", row[1]);
                    courseMap.put("instructor", row[2] != null ? row[2] : "Staff");
                    courseMap.put("progressPct", row[3] != null ? row[3] : 0);
                    courses.add(courseMap);
                    System.out.println("  Added course: " + row[1] + " (Progress: " + row[3] + "%)");
                }
            }

            // 2. Get PENDING submissions (including those without any submission record)
            List<Object[]> pendingResults = submissionRepository.findPendingSubmissionsForStudentNative(studentId);
            System.out.println("Pending assignments found: " + pendingResults.size());

            List<Map<String, Object>> upcomingAssignments = new ArrayList<>();

            for (Object[] row : pendingResults) {
                Map<String, Object> assignmentMap = new HashMap<>();
                assignmentMap.put("id", row[1]);           // assignment_id (index 1)
                assignmentMap.put("title", row[2]);        // title (index 2)
                assignmentMap.put("dueDate", row[3]);      // due_date (index 3)
                assignmentMap.put("score", row[4]);        // score (index 4)

                // Handle status - it could be String or Number
                Object statusObj = row[5];
                String status;
                if (statusObj instanceof String) {
                    status = (String) statusObj;
                } else {
                    // If it's a number (like 0,1,2), convert to string representation
                    status = String.valueOf(statusObj);
                }
                assignmentMap.put("status", status);

                assignmentMap.put("courseName", row[6]);   // course_name (index 6)
                upcomingAssignments.add(assignmentMap);
                System.out.println("  Added pending assignment: " + row[2] + " (Course: " + row[6] + ", Status: " + status + ")");
            }

            // 3. Get ALL submissions for stats (completed assignments, average grade)
            List<Object[]> allSubmissionResults = submissionRepository.findSubmissionsWithDetailsNative(studentId);
            long completedAssignments = 0;
            double totalScore = 0;
            int gradedCount = 0;
            int totalAssignments = allSubmissionResults.size();

            for (Object[] row : allSubmissionResults) {
                // Status is at index 5 in this query
                Object statusObj = row[5];
                String status;
                if (statusObj instanceof String) {
                    status = (String) statusObj;
                } else {
                    status = String.valueOf(statusObj);
                }

                if ("GRADED".equals(status)) {
                    completedAssignments++;
                    Object scoreObj = row[4];
                    if (scoreObj != null) {
                        double score = ((Number) scoreObj).doubleValue();
                        totalScore += score;
                        gradedCount++;
                    }
                }
            }

            double avgGrade = gradedCount > 0 ? (totalScore / gradedCount) : 0;

            model.addAttribute("studentName", student.getName());
            model.addAttribute("activeCourses", activeCourses);
            model.addAttribute("completedCourses", completedCourses);
            model.addAttribute("completedAssignments", completedAssignments);
            model.addAttribute("totalAssignments", totalAssignments);
            model.addAttribute("averageGrade", Math.round(avgGrade * 10) / 10.0);
            model.addAttribute("courses", courses);
            model.addAttribute("upcomingAssignments", upcomingAssignments);
            model.addAttribute("title", "Student Dashboard");

            System.out.println("Dashboard data - Active: " + activeCourses +
                    ", Courses: " + courses.size() +
                    ", Pending Assignments: " + upcomingAssignments.size() +
                    ", Completed: " + completedAssignments);

        } catch (Exception e) {
            System.err.println("ERROR in student dashboard: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("activeCourses", 0);
            model.addAttribute("completedCourses", 0);
            model.addAttribute("completedAssignments", 0);
            model.addAttribute("totalAssignments", 0);
            model.addAttribute("averageGrade", 0);
            model.addAttribute("courses", new ArrayList<>());
            model.addAttribute("upcomingAssignments", new ArrayList<>());
        }

        return "student-dashboard";
    }

    @GetMapping("/courses")
    public String courses(@RequestParam(required = false) String keyword,
                          @RequestParam(defaultValue = "0") int page,
                          @RequestParam(defaultValue = "20") int size,  // Increased to show all
                          @AuthenticationPrincipal UserDetails userDetails,
                          Model model) {
        if (userDetails == null) return "redirect:/login";

        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());

            // Get enrolled course IDs
            Set<Long> enrolledCourseIds = enrollmentRepository.findByStudentId(student.getId())
                    .stream().map(e -> e.getCourse().getId()).collect(Collectors.toSet());

            System.out.println("=== COURSE CATALOG DEBUG ===");
            System.out.println("Student ID: " + student.getId());
            System.out.println("Enrolled course IDs: " + enrolledCourseIds);

            // Get ALL courses (increase size to show all)
            Pageable pageable = PageRequest.of(page, size, Sort.by("name").ascending());
            PageResponseDto<CourseResponseDto> coursePage;

            if (keyword != null && !keyword.isBlank()) {
                coursePage = courseService.search(keyword, pageable);
                model.addAttribute("keyword", keyword);
            } else {
                coursePage = courseService.findAll(pageable);
            }

            System.out.println("Total courses found: " + coursePage.getContent().size());

            List<Map<String, Object>> enhancedCourses = new ArrayList<>();
            for (CourseResponseDto course : coursePage.getContent()) {
                Map<String, Object> enhanced = new HashMap<>();
                enhanced.put("id", course.getId());
                enhanced.put("name", course.getName());
                enhanced.put("description", course.getDescription() != null ? course.getDescription() : "");
                enhanced.put("instructor", course.getInstructor() != null ? course.getInstructor() : "Staff");
                enhanced.put("moduleCount", course.getModuleCount());
                enhanced.put("enrollmentCount", course.getEnrollmentCount());
                enhanced.put("enrolled", enrolledCourseIds.contains(course.getId()));

                // If enrolled, get progress
                if (enrolledCourseIds.contains(course.getId())) {
                    var enrollment = enrollmentRepository.findByStudentIdAndCourseId(student.getId(), course.getId());
                    enhanced.put("progressPct", enrollment.map(e -> e.getProgressPct()).orElse(BigDecimal.ZERO));
                } else {
                    enhanced.put("progressPct", 0);
                }

                enhancedCourses.add(enhanced);
                System.out.println("  Course: " + course.getName() + " - Enrolled: " + enhanced.get("enrolled"));
            }

            model.addAttribute("courses", enhancedCourses);
            model.addAttribute("page", coursePage);
            model.addAttribute("title", "Course Catalog");

        } catch (Exception e) {
            System.err.println("ERROR in course catalog: " + e.getMessage());
            e.printStackTrace();
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
            Long studentId = student.getId();

            CourseResponseDto course = courseService.findById(id);
            List<ModuleResponseDto> modules = moduleService.findByCourse(id);

            // Get all submissions for this student to determine assignment status
            List<Submission> submissions = submissionRepository.findByStudentId(studentId);
            Map<Long, Submission> submissionMap = submissions.stream()
                    .collect(Collectors.toMap(s -> s.getAssignment().getId(), s -> s));

            // Build enhanced modules with assignments and status
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
                        enhancedAssignment.put("feedback", submission.getFeedback());
                    } else {
                        enhancedAssignment.put("status", "PENDING");
                        enhancedAssignment.put("score", null);
                        enhancedAssignment.put("feedback", null);
                    }
                    enhancedAssignments.add(enhancedAssignment);
                }
                enhancedModule.put("assignments", enhancedAssignments);
                enhancedModules.add(enhancedModule);
            }

            // Get enrollment for progress
            var enrollment = enrollmentRepository.findByStudentIdAndCourseId(studentId, id).orElse(null);
            BigDecimal progressPct = enrollment != null ? enrollment.getProgressPct() : BigDecimal.ZERO;
            BigDecimal grade = enrollment != null ? enrollment.getGrade() : null;

            model.addAttribute("course", course);
            model.addAttribute("modules", enhancedModules);
            model.addAttribute("moduleCount", modules.size());
            model.addAttribute("enrollmentCount", course.getEnrollmentCount());
            model.addAttribute("progressPct", progressPct);
            model.addAttribute("grade", grade);
            model.addAttribute("title", course.getName());

            System.out.println("Course Detail - Modules: " + modules.size());

        } catch (Exception e) {
            System.err.println("Error in courseDetail: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", "Course not found: " + e.getMessage());
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
            System.out.println("=== ENROLL DEBUG ===");
            System.out.println("Student ID: " + student.getId());
            System.out.println("Course ID: " + courseId);

            EnrollmentRequestDto dto = new EnrollmentRequestDto();
            dto.setStudentId(student.getId());
            dto.setCourseId(courseId);
            enrollmentService.enroll(dto);

            redirectAttributes.addFlashAttribute("message", "Successfully enrolled in course!");
            System.out.println("Enrollment successful!");

        } catch (Exception e) {
            System.err.println("Enrollment error: " + e.getMessage());
            e.printStackTrace();
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
                                   @RequestParam(required = false) String comment,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());

            // Store file if provided
            String filePath = null;
            if (file != null && !file.isEmpty()) {
                filePath = storageService.store(file, student.getId(), assignmentId);
            }

            // Create submission DTO
            var submissionDto = new com.vbforge.educationapi.dto.submission.SubmissionRequestDto();
            submissionDto.setAssignmentId(assignmentId);
            submissionDto.setStudentId(student.getId());

            // Submit the assignment
            submissionService.submit(submissionDto, filePath);

            // Also store comment if needed (you may need to add comment field to Submission entity)

            redirectAttributes.addFlashAttribute("message", "Assignment submitted successfully!");

            // Get the course ID for redirect
            AssignmentResponseDto assignment = assignmentService.findById(assignmentId);
            Long courseId = assignment.getCourseId();

            System.out.println("Submission successful! Redirecting to course: " + courseId);

            // Redirect to course detail page
            return "redirect:/student/courses/" + courseId;

        } catch (Exception e) {
            System.err.println("Submission error: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Submission failed: " + e.getMessage());
            return "redirect:/student/assignments/" + assignmentId + "/submit";
        }
    }


    @GetMapping("/assignments")
    public String viewAllAssignments(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";

        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            Long studentId = student.getId();

            List<Object[]> results = submissionRepository.findSubmissionsWithDetailsNative(studentId);

            List<Map<String, Object>> assignments = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> assignment = new HashMap<>();
                assignment.put("submissionId", row[0]);  // submission ID (for download)
                assignment.put("id", row[1]);            // assignment ID (for submit link)
                assignment.put("title", row[2]);
                assignment.put("dueDate", row[3]);
                assignment.put("score", row[4]);
                assignment.put("status", row[5]);
                assignment.put("courseName", row[6]);
                assignment.put("pointsPossible", row[7]);
                assignment.put("filePath", row[8]);
                assignments.add(assignment);
            }

            model.addAttribute("assignments", assignments);
            model.addAttribute("title", "My Assignments");

        } catch (Exception e) {
            System.err.println("Error in viewAllAssignments: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("assignments", new ArrayList<>());
        }

        return "student-assignments";
    }

    @GetMapping("/grades")
    public String viewGrades(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";

        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            Long studentId = student.getId();

            // Get all graded submissions with details using native query
            List<Object[]> results = submissionRepository.findGradedSubmissionsWithDetailsNative(studentId);

            List<Map<String, Object>> gradedAssignments = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> assignment = new HashMap<>();
                assignment.put("title", row[0]);      // assignment title
                assignment.put("courseName", row[1]); // course name
                assignment.put("score", row[2]);      // score
                assignment.put("pointsPossible", row[3]); // points possible
                assignment.put("feedback", row[4]);   // feedback
                assignment.put("submittedAt", row[5]); // submitted at
                assignment.put("gradedAt", row[6]);   // graded at
                gradedAssignments.add(assignment);
            }

            // Calculate overall average
            double totalPercentage = 0;
            int gradedCount = 0;
            for (Map<String, Object> assignment : gradedAssignments) {
                Number score = (Number) assignment.get("score");
                Number points = (Number) assignment.get("pointsPossible");
                if (score != null && points != null && points.doubleValue() > 0) {
                    totalPercentage += (score.doubleValue() / points.doubleValue()) * 100;
                    gradedCount++;
                }
            }
            double overallAverage = gradedCount > 0 ? totalPercentage / gradedCount : 0;

            model.addAttribute("gradedAssignments", gradedAssignments);
            model.addAttribute("overallAverage", Math.round(overallAverage));
            model.addAttribute("totalGraded", gradedCount);
            model.addAttribute("title", "My Grades");

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "student-grades";
    }


    @GetMapping("/announcements")
    public String viewAnnouncements(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";

        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());
            System.out.println("=== STUDENT ANNOUNCEMENTS DEBUG ===");
            System.out.println("Student: " + student.getEmail());

            // Use native query to get announcements directly with course names
            List<Object[]> results = announcementRepository.findAnnouncementsForStudentNative(student.getId());

            List<Map<String, Object>> announcements = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> annMap = new HashMap<>();
                annMap.put("title", row[0]);      // announcement title
                annMap.put("message", row[1]);    // announcement message
                annMap.put("courseName", row[2]); // course name
                annMap.put("postedAt", row[3]);   // posted at
                announcements.add(annMap);
                System.out.println("Added announcement: " + row[0] + " for course: " + row[2]);
            }

            System.out.println("Total announcements for student: " + announcements.size());

            model.addAttribute("announcements", announcements);
            model.addAttribute("title", "Announcements");

        } catch (Exception e) {
            System.err.println("Error in viewAnnouncements: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("announcements", new ArrayList<>());
        }

        return "student-announcements";
    }


    @GetMapping("/download/{submissionId}")
    public ResponseEntity<Resource> downloadSubmission(@PathVariable Long submissionId,
                                                       @AuthenticationPrincipal UserDetails userDetails) {
        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());

            Submission submission = submissionRepository.findById(submissionId)
                    .orElseThrow(() -> new RuntimeException("Submission not found"));

            // Security: Only the student who submitted can download
            if (!submission.getStudent().getId().equals(student.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
            }

            if (submission.getFilePath() == null || submission.getFilePath().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            Path filePath = storageService.load(submission.getFilePath());
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() && !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            if (contentType == null) {
                contentType = "application/octet-stream";
            }

            // Extract filename from path
            String filename = filePath.getFileName().toString();
            String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8)
                    .replaceAll("\\+", "%20");

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "attachment; filename*=UTF-8''" + encodedFilename)
                    .body(resource);

        } catch (Exception e) {
            System.err.println("Error downloading submission: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.internalServerError().build();
        }
    }

}












