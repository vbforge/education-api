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
import com.vbforge.educationapi.dto.course.CourseRequestDto;
import com.vbforge.educationapi.dto.course.CourseResponseDto;
import com.vbforge.educationapi.dto.enrollment.EnrollmentRequestDto;
import com.vbforge.educationapi.dto.module.ModuleResponseDto;
import com.vbforge.educationapi.dto.submission.SubmissionRequestDto;
import com.vbforge.educationapi.repository.CourseRepository;
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
import org.springframework.web.multipart.MultipartFile;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final AssignmentService assignmentService;
    private final StorageService storageService;
    private final StudentService studentService;
    private final CourseService courseService;
    private final ModuleService moduleService;
    private final EnrollmentService enrollmentService;
    private final EnrollmentRepository enrollmentRepository;
    private final SubmissionRepository submissionRepository;
    private final SubmissionService submissionService;
    private final CourseRepository courseRepository;
    private final ProgressService progressService;


    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/")
    public String home(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        // Check user role and redirect to appropriate dashboard
        boolean isAdmin = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isInstructor = userDetails.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_INSTRUCTOR"));

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        } else if (isInstructor) {
            return "redirect:/instructor/dashboard";
        } else {
            return "redirect:/dashboard";
        }
    }

    @GetMapping("/dashboard")
    @Transactional(readOnly = true)
    public String studentDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
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

    @GetMapping("/instructor/dashboard")
    public String instructorDashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            // Get instructor email
            String instructorEmail = userDetails.getUsername();

            // Get courses WHERE instructor = instructorEmail
            List<Course> allCourses = courseRepository.findAll();
            List<Course> myCourses = allCourses.stream()
                    .filter(c -> instructorEmail.equals(c.getInstructor()) ||
                            (c.getInstructor() != null && c.getInstructor().contains(instructorEmail)))
                    .collect(Collectors.toList());

            List<Map<String, Object>> courseList = new ArrayList<>();
            int totalStudents = 0;
            int pendingGrading = 0;

            for (Course course : myCourses) {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("id", course.getId());
                courseMap.put("name", course.getName());

                int enrollmentCount = courseRepository.countEnrollmentsByCourseId(course.getId());
                int moduleCount = courseRepository.countModulesByCourseId(course.getId());

                courseMap.put("enrollmentCount", enrollmentCount);
                courseMap.put("moduleCount", moduleCount);

                totalStudents += enrollmentCount;

                int pending = submissionRepository.findByCourseIdAndStatus(course.getId(), SubmissionStatus.SUBMITTED).size();
                courseMap.put("pendingSubmissions", pending);
                pendingGrading += pending;

                courseList.add(courseMap);
            }

            // Calculate average completion across enrollments in my courses
            double avgCompletion = 0;
            int totalProgress = 0;
            int enrollmentCount = 0;
            for (Course course : myCourses) {
                List<Enrollment> enrollments = enrollmentRepository.findByCourseId(course.getId());
                for (Enrollment e : enrollments) {
                    totalProgress += e.getProgressPct().doubleValue();
                    enrollmentCount++;
                }
            }
            avgCompletion = enrollmentCount > 0 ? (double) totalProgress / enrollmentCount : 0;

            // Get recent pending submissions from my courses
            List<Map<String, Object>> pendingSubmissions = new ArrayList<>();
            for (Course course : myCourses) {
                List<Submission> subs = submissionRepository.findByCourseIdAndStatus(course.getId(), SubmissionStatus.SUBMITTED);
                for (Submission submission : subs.stream().limit(5).toList()) {
                    Map<String, Object> subMap = new HashMap<>();
                    subMap.put("id", submission.getId());
                    subMap.put("assignmentTitle", submission.getAssignment().getTitle());
                    subMap.put("courseName", course.getName());
                    subMap.put("studentName", submission.getStudent().getName());
                    subMap.put("submittedAt", submission.getSubmittedAt());
                    pendingSubmissions.add(subMap);
                }
            }

            model.addAttribute("courseCount", myCourses.size());
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("pendingGrading", pendingGrading);
            model.addAttribute("avgCompletion", Math.round(avgCompletion));
            model.addAttribute("courses", courseList);
            model.addAttribute("pendingSubmissions", pendingSubmissions.stream().limit(5).toList());
            model.addAttribute("title", "Instructor Dashboard");

        } catch (Exception e) {
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
        }

        return "instructor-dashboard";
    }

    // Keep the old mapping for compatibility
    @GetMapping("/student/dashboard")
    @Transactional(readOnly = true)
    public String studentDashboardRedirect(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        return studentDashboard(userDetails, model);
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

    // Show assignment submission page
    @GetMapping("/assignments/{id}/submit")
    public String showSubmitForm(@PathVariable Long id,
                                 @AuthenticationPrincipal UserDetails userDetails,
                                 Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());

            // Get assignment details
            AssignmentResponseDto assignment = assignmentService.findById(id);

            // Get course info
            Long courseId = assignment.getCourseId();
            CourseResponseDto course = courseService.findById(courseId);

            // Check if student has already submitted
            var existingSubmission = submissionRepository
                    .findByAssignmentIdAndStudentId(id, student.getId())
                    .orElse(null);

            model.addAttribute("assignment", assignment);
            model.addAttribute("courseId", courseId);
            model.addAttribute("courseName", course.getName());
            model.addAttribute("existingSubmission", existingSubmission);
            model.addAttribute("title", assignment.getTitle());

            if (existingSubmission != null && existingSubmission.getStatus() == SubmissionStatus.GRADED) {
                model.addAttribute("alreadyGraded", true);
            }

        } catch (Exception e) {
            model.addAttribute("error", "Assignment not found: " + e.getMessage());
            return "redirect:/dashboard";
        }

        return "assignment-submit";
    }

    // Handle assignment submission
    @PostMapping("/submit-assignment")
    public String submitAssignment(@RequestParam Long assignmentId,
                                   @RequestParam(value = "file", required = false) MultipartFile file,
                                   @RequestParam(value = "comment", required = false) String comment,
                                   @AuthenticationPrincipal UserDetails userDetails,
                                   RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Student student = studentService.getStudentOrThrowByEmail(userDetails.getUsername());

            // Check if already submitted
            if (submissionRepository.existsByAssignmentIdAndStudentId(assignmentId, student.getId())) {
                redirectAttributes.addFlashAttribute("warning", "You have already submitted this assignment. New version will overwrite.");
            }

            // Handle file upload
            String filePath = null;
            if (file != null && !file.isEmpty()) {
                filePath = storageService.store(file, student.getId(), assignmentId);
            }

            // Create or update submission
            SubmissionRequestDto submissionDto = new SubmissionRequestDto();
            submissionDto.setAssignmentId(assignmentId);
            submissionDto.setStudentId(student.getId());

            submissionService.submit(submissionDto, filePath);

            redirectAttributes.addFlashAttribute("message", "Assignment submitted successfully!");

            // Get course ID for redirect
            AssignmentResponseDto assignment = assignmentService.findById(assignmentId);
            return "redirect:/courses/" + assignment.getCourseId();

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to submit: " + e.getMessage());
            return "redirect:/assignments/" + assignmentId + "/submit";
        }
    }



    @GetMapping("/instructor/courses")
    public String instructorCourses(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            String instructorEmail = userDetails.getUsername();

            // Filter courses by instructor
            List<Course> allCourses = courseRepository.findAll();
            List<Course> myCourses = allCourses.stream()
                    .filter(c -> instructorEmail.equals(c.getInstructor()) ||
                            (c.getInstructor() != null && c.getInstructor().contains(instructorEmail)))
                    .collect(Collectors.toList());

            List<Map<String, Object>> courseList = new ArrayList<>();

            for (Course course : myCourses) {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("id", course.getId());
                courseMap.put("name", course.getName());
                courseMap.put("enrollmentCount", courseRepository.countEnrollmentsByCourseId(course.getId()));
                courseMap.put("moduleCount", courseRepository.countModulesByCourseId(course.getId()));

                int pending = submissionRepository.findByCourseIdAndStatus(course.getId(), SubmissionStatus.SUBMITTED).size();
                courseMap.put("pendingSubmissions", pending);

                courseList.add(courseMap);
            }

            model.addAttribute("courses", courseList);
            model.addAttribute("title", "My Courses");

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("courses", new ArrayList<>());
        }

        return "instructor-courses";
    }

    @GetMapping("/instructor/courses/create")
    public String showCreateCourseForm(Model model) {
        model.addAttribute("title", "Create Course");
        return "instructor-course-form";
    }

    @PostMapping("/instructor/courses/create")
    public String createCourse(@RequestParam String name,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String instructor,
                               @RequestParam(required = false) String syllabus,
                               @RequestParam(required = false) String schedule,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            CourseRequestDto courseDto = new CourseRequestDto();
            courseDto.setName(name);
            courseDto.setDescription(description);
            courseDto.setInstructor(instructor != null ? instructor : userDetails.getUsername());
            courseDto.setSyllabus(syllabus);
            courseDto.setSchedule(schedule);

            courseService.create(courseDto);
            redirectAttributes.addFlashAttribute("message", "Course created successfully!");

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create course: " + e.getMessage());
        }

        return "redirect:/instructor/courses";
    }

    @GetMapping("/instructor/courses/{id}")
    public String manageCourse(@PathVariable Long id, Model model) {
        try {
            CourseResponseDto course = courseService.findById(id);
            model.addAttribute("course", course);
            model.addAttribute("title", "Manage Course - " + course.getName());
            return "instructor-course-manage";
        } catch (Exception e) {
            model.addAttribute("error", "Course not found");
            return "redirect:/instructor/courses";
        }
    }

    @GetMapping("/instructor/courses/{id}/grade")
    public String gradeSubmissions(@PathVariable Long id, Model model) {
        try {
            CourseResponseDto course = courseService.findById(id);

            // Get pending submissions for this course
            List<Submission> pendingSubmissions = submissionRepository.findByCourseIdAndStatus(id, SubmissionStatus.SUBMITTED);

            // Convert to map for template
            List<Map<String, Object>> submissionList = new ArrayList<>();
            for (Submission submission : pendingSubmissions) {
                Map<String, Object> subMap = new HashMap<>();
                subMap.put("id", submission.getId());
                subMap.put("assignmentTitle", submission.getAssignment().getTitle());
                subMap.put("studentName", submission.getStudent().getName());
                subMap.put("submittedAt", submission.getSubmittedAt());
                subMap.put("status", submission.getStatus().name());
                submissionList.add(subMap);
            }

            model.addAttribute("course", course);
            model.addAttribute("submissions", submissionList);
            model.addAttribute("title", "Grade Submissions - " + course.getName());

        } catch (Exception e) {
            model.addAttribute("error", "Course not found: " + e.getMessage());
            return "redirect:/instructor/courses";
        }

        return "instructor-grading";
    }

    @GetMapping("/instructor/submissions/{id}/grade")
    public String showGradeForm(@PathVariable Long id,
                                @AuthenticationPrincipal UserDetails userDetails,
                                Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Submission submission = submissionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Submission not found"));

            Map<String, Object> submissionMap = new HashMap<>();
            submissionMap.put("id", submission.getId());
            submissionMap.put("studentName", submission.getStudent().getName());
            submissionMap.put("assignmentTitle", submission.getAssignment().getTitle());
            submissionMap.put("courseName", submission.getAssignment().getModule().getCourse().getName());
            submissionMap.put("submittedAt", submission.getSubmittedAt());
            submissionMap.put("filePath", submission.getFilePath());
            submissionMap.put("score", submission.getScore());
            submissionMap.put("feedback", submission.getFeedback());
            submissionMap.put("status", submission.getStatus().name());

            model.addAttribute("submission", submissionMap);
            model.addAttribute("assignmentPoints", submission.getAssignment().getPointsPossible());
            model.addAttribute("courseId", submission.getAssignment().getModule().getCourse().getId());
            model.addAttribute("title", "Grade Submission");

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "redirect:/instructor/dashboard";
        }

        return "instructor-grade-submission";
    }

    @PostMapping("/instructor/submissions/{id}/grade")
    public String saveGrade(@PathVariable Long id,
                            @RequestParam Double score,
                            @RequestParam(required = false) String feedback,
                            @AuthenticationPrincipal UserDetails userDetails,
                            RedirectAttributes redirectAttributes) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Submission submission = submissionRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Submission not found"));

            submission.setScore(BigDecimal.valueOf(score));
            submission.setFeedback(feedback);
            submission.setStatus(SubmissionStatus.GRADED);
            submissionRepository.save(submission);

            // Recalculate progress for the student
            Long studentId = submission.getStudent().getId();
            Long courseId = submission.getAssignment().getModule().getCourse().getId();
            progressService.recalculate(studentId, courseId);

            redirectAttributes.addFlashAttribute("message", "Grade saved successfully!");

            return "redirect:/instructor/courses/" + courseId + "/grade";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to save grade: " + e.getMessage());
            return "redirect:/instructor/submissions/" + id + "/grade";
        }
    }


}