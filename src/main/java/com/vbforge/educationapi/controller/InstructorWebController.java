package com.vbforge.educationapi.controller;

import com.vbforge.educationapi.domain.*;
import com.vbforge.educationapi.dto.assignment.AssignmentRequestDto;
import com.vbforge.educationapi.dto.assignment.AssignmentResponseDto;
import com.vbforge.educationapi.dto.course.CourseRequestDto;
import com.vbforge.educationapi.dto.course.CourseResponseDto;
import com.vbforge.educationapi.dto.module.ModuleRequestDto;
import com.vbforge.educationapi.dto.module.ModuleResponseDto;
import com.vbforge.educationapi.repository.*;
import com.vbforge.educationapi.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/instructor")
@RequiredArgsConstructor
public class InstructorWebController {

    private final CourseService courseService;
    private final ModuleService moduleService;
    private final AssignmentService assignmentService;
    private final SubmissionService submissionService;
    private final StudentService studentService;
    private final ProgressService progressService;
    private final CourseRepository courseRepository;
    private final ModuleRepository moduleRepository;
    private final AssignmentRepository assignmentRepository;
    private final SubmissionRepository submissionRepository;
    private final EnrollmentRepository enrollmentRepository;

    // ============================================
    // DASHBOARD
    // ============================================

    @GetMapping("/dashboard")
    public String dashboard(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";

        try {
            String instructorEmail = userDetails.getUsername();
            System.out.println("=== INSTRUCTOR DASHBOARD DEBUG ===");
            System.out.println("Logged in email: '" + instructorEmail + "'");

            // Get ALL courses to see what's in DB
            List<Course> allCourses = courseRepository.findAll();
            System.out.println("Total courses in DB: " + allCourses.size());

            for (Course c : allCourses) {
                System.out.println("  Course: '" + c.getName() + "', Instructor: '" + c.getInstructor() + "'");
            }

            // Filter courses by instructor (trim and case-insensitive)
            List<Course> myCourses = allCourses.stream()
                    .filter(c -> c.getInstructor() != null &&
                            instructorEmail.equalsIgnoreCase(c.getInstructor().trim()))
                    .collect(Collectors.toList());

            System.out.println("Courses matching instructor: " + myCourses.size());

            List<Map<String, Object>> courseList = new ArrayList<>();
            int totalStudents = 0;
            int pendingGrading = 0;

            for (Course course : myCourses) {
                System.out.println("Processing course: " + course.getName());

                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("id", course.getId());
                courseMap.put("name", course.getName());

                int enrollmentCount = courseRepository.countEnrollmentsByCourseId(course.getId());
                int moduleCount = courseRepository.countModulesByCourseId(course.getId());
                int pending = submissionRepository.findByCourseIdAndStatus(course.getId(), SubmissionStatus.SUBMITTED).size();

                System.out.println("  Enrollment count: " + enrollmentCount);
                System.out.println("  Module count: " + moduleCount);
                System.out.println("  Pending submissions: " + pending);

                courseMap.put("enrollmentCount", enrollmentCount);
                courseMap.put("moduleCount", moduleCount);
                courseMap.put("pendingSubmissions", pending);

                totalStudents += enrollmentCount;
                pendingGrading += pending;
                courseList.add(courseMap);
            }

            System.out.println("Final courseList size: " + courseList.size());

            model.addAttribute("courseCount", myCourses.size());
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("pendingGrading", pendingGrading);
            model.addAttribute("avgCompletion", 0);
            model.addAttribute("courses", courseList);
            model.addAttribute("pendingSubmissions", new ArrayList<>());
            model.addAttribute("title", "Instructor Dashboard");

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
        }

        return "instructor-dashboard";
    }

    // ============================================
    // COURSE MANAGEMENT
    // ============================================
    
    @GetMapping("/courses")
    public String myCourses(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) return "redirect:/login";
        
        try {
            String instructorEmail = userDetails.getUsername();
            List<Course> allCourses = courseRepository.findAll();
            List<Course> myCourses = allCourses.stream()
                    .filter(c -> c.getInstructor() != null && instructorEmail.equalsIgnoreCase(c.getInstructor().trim()))
                    .collect(Collectors.toList());
            
            List<Map<String, Object>> courseList = new ArrayList<>();
            for (Course course : myCourses) {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("id", course.getId());
                courseMap.put("name", course.getName());
                courseMap.put("enrollmentCount", courseRepository.countEnrollmentsByCourseId(course.getId()));
                courseMap.put("moduleCount", courseRepository.countModulesByCourseId(course.getId()));
                courseMap.put("pendingSubmissions", submissionRepository.findByCourseIdAndStatus(course.getId(), SubmissionStatus.SUBMITTED).size());
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
    
    @GetMapping("/courses/create")
    public String showCreateCourseForm(Model model) {
        model.addAttribute("title", "Create Course");
        return "instructor-course-form";
    }
    
    @PostMapping("/courses/create")
    public String createCourse(@RequestParam String name,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String instructor,
                               @RequestParam(required = false) String syllabus,
                               @RequestParam(required = false) String schedule,
                               @AuthenticationPrincipal UserDetails userDetails,
                               RedirectAttributes redirectAttributes) {
        try {
            CourseRequestDto dto = new CourseRequestDto();
            dto.setName(name);
            dto.setDescription(description);
            dto.setInstructor(instructor != null ? instructor : userDetails.getUsername());
            dto.setSyllabus(syllabus);
            dto.setSchedule(schedule);
            
            courseService.create(dto);
            redirectAttributes.addFlashAttribute("message", "Course created successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create course: " + e.getMessage());
        }
        
        return "redirect:/instructor/courses";
    }
    
    @GetMapping("/courses/{id}/edit")
    public String editCourse(@PathVariable Long id, Model model) {
        try {
            CourseResponseDto course = courseService.findById(id);
            model.addAttribute("course", course);
            model.addAttribute("title", "Edit Course - " + course.getName());
            return "instructor-course-edit";
        } catch (Exception e) {
            return "redirect:/instructor/courses";
        }
    }
    
    @PostMapping("/courses/{id}/edit")
    public String updateCourse(@PathVariable Long id,
                               @RequestParam String name,
                               @RequestParam(required = false) String description,
                               @RequestParam(required = false) String syllabus,
                               @RequestParam(required = false) String schedule,
                               RedirectAttributes redirectAttributes) {
        try {
            CourseRequestDto dto = new CourseRequestDto();
            dto.setName(name);
            dto.setDescription(description);
            dto.setSyllabus(syllabus);
            dto.setSchedule(schedule);
            
            courseService.update(id, dto);
            redirectAttributes.addFlashAttribute("message", "Course updated successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to update course: " + e.getMessage());
        }
        
        return "redirect:/instructor/courses";
    }

    // ============================================
    // MODULE MANAGEMENT
    // ============================================
    
    @GetMapping("/courses/{courseId}/modules")
    public String manageModules(@PathVariable Long courseId, Model model) {
        try {
            CourseResponseDto course = courseService.findById(courseId);
            List<ModuleResponseDto> modules = moduleService.findByCourse(courseId);
            
            model.addAttribute("course", course);
            model.addAttribute("modules", modules);
            model.addAttribute("title", "Manage Modules - " + course.getName());
            return "instructor-modules";
        } catch (Exception e) {
            return "redirect:/instructor/courses";
        }
    }
    
    @PostMapping("/courses/{courseId}/modules/create")
    public String createModule(@PathVariable Long courseId,
                               @RequestParam String title,
                               @RequestParam(required = false) String content,
                               @RequestParam(defaultValue = "0") int orderIndex,
                               RedirectAttributes redirectAttributes) {
        try {
            ModuleRequestDto dto = new ModuleRequestDto();
            dto.setCourseId(courseId);
            dto.setTitle(title);
            dto.setContent(content);
            dto.setOrderIndex(orderIndex);
            
            moduleService.create(dto);
            redirectAttributes.addFlashAttribute("message", "Module created successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create module: " + e.getMessage());
        }
        
        return "redirect:/instructor/courses/" + courseId + "/modules";
    }
    
    @PostMapping("/modules/{id}/delete")
    public String deleteModule(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            ModuleResponseDto module = moduleService.findById(id);
            Long courseId = module.getCourseId();
            moduleService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Module deleted successfully!");
            return "redirect:/instructor/courses/" + courseId + "/modules";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete module: " + e.getMessage());
            return "redirect:/instructor/courses";
        }
    }

    // ============================================
    // ASSIGNMENT MANAGEMENT
    // ============================================
    
    @GetMapping("/modules/{moduleId}/assignments")
    public String manageAssignments(@PathVariable Long moduleId, Model model) {
        try {
            ModuleResponseDto module = moduleService.findById(moduleId);
            List<AssignmentResponseDto> assignments = assignmentService.findByModule(moduleId);
            
            model.addAttribute("module", module);
            model.addAttribute("assignments", assignments);
            model.addAttribute("title", "Manage Assignments - " + module.getTitle());
            return "instructor-assignments";
        } catch (Exception e) {
            return "redirect:/instructor/courses";
        }
    }
    
    @PostMapping("/modules/{moduleId}/assignments/create")
    public String createAssignment(@PathVariable Long moduleId,
                                   @RequestParam String title,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(required = false) String dueDate,
                                   @RequestParam(defaultValue = "100") int pointsPossible,
                                   RedirectAttributes redirectAttributes) {
        try {
            AssignmentRequestDto dto = new AssignmentRequestDto();
            dto.setModuleId(moduleId);
            dto.setTitle(title);
            dto.setDescription(description);
            dto.setPointsPossible(pointsPossible);
            if (dueDate != null && !dueDate.isEmpty()) {
                dto.setDueDate(java.time.LocalDateTime.parse(dueDate + "T23:59:59"));
            }
            
            assignmentService.create(dto);
            redirectAttributes.addFlashAttribute("message", "Assignment created successfully!");
            
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to create assignment: " + e.getMessage());
        }
        
        return "redirect:/instructor/modules/" + moduleId + "/assignments";
    }

    // ============================================
    // GRADING
    // ============================================

    @GetMapping("/courses/{courseId}/grade")
    public String gradeSubmissions(@PathVariable Long courseId, Model model) {
        try {
            System.out.println("=== GRADE SUBMISSIONS NATIVE QUERY ===");
            System.out.println("Course ID: " + courseId);

            // Get course using native query to avoid lazy loading issues
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                System.out.println("Course not found!");
                model.addAttribute("error", "Course not found");
                return "instructor-grading";
            }

            System.out.println("Course name: " + course.getName());

            // Use native query to get pending submissions
            List<Object[]> results = submissionRepository.findPendingSubmissionsByCourseIdNative(courseId);
            System.out.println("Native query results count: " + results.size());

            List<Map<String, Object>> submissionList = new ArrayList<>();
            for (Object[] row : results) {
                Map<String, Object> subMap = new HashMap<>();
                subMap.put("id", row[0]);
                subMap.put("assignmentTitle", row[1]);
                subMap.put("studentName", row[2]);
                subMap.put("submittedAt", row[3]);
                submissionList.add(subMap);
                System.out.println("  - Submission ID: " + row[0] + ", Assignment: " + row[1] + ", Student: " + row[2]);
            }

            model.addAttribute("courseName", course.getName());
            model.addAttribute("courseId", courseId);
            model.addAttribute("submissions", submissionList);
            model.addAttribute("title", "Grade Submissions - " + course.getName());

            System.out.println("Returning instructor-grading with " + submissionList.size() + " submissions");

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("submissions", new ArrayList<>());
        }

        return "instructor-grading";
    }

    @GetMapping("/submissions/{id}/grade")
    public String showGradeForm(@PathVariable Long id, Model model) {
        try {
            System.out.println("=== SHOW GRADE FORM NATIVE ===");
            System.out.println("Submission ID: " + id);

            List<Object[]> results = submissionRepository.findSubmissionByIdNative(id);

            if (results.isEmpty()) {
                System.out.println("Submission not found!");
                return "redirect:/instructor/dashboard";
            }

            Object[] row = results.get(0);

            Map<String, Object> submissionMap = new HashMap<>();
            submissionMap.put("id", row[0]);  // submission id
            submissionMap.put("assignmentTitle", row[1]);
            submissionMap.put("assignmentPoints", row[2]);
            submissionMap.put("studentName", row[3]);
            submissionMap.put("courseName", row[4]);
            submissionMap.put("courseId", row[5]);
            submissionMap.put("submittedAt", row[6]);
            submissionMap.put("filePath", row[7]);
            submissionMap.put("score", row[8]);
            submissionMap.put("feedback", row[9]);

            System.out.println("Submission found: " + submissionMap.get("assignmentTitle"));
            System.out.println("Course ID: " + submissionMap.get("courseId"));

            model.addAttribute("submission", submissionMap);
            model.addAttribute("assignmentPoints", row[2]);
            model.addAttribute("courseId", row[5]);
            model.addAttribute("title", "Grade Submission - " + row[1]);

        } catch (Exception e) {
            System.err.println("ERROR in showGradeForm: " + e.getMessage());
            e.printStackTrace();
            return "redirect:/instructor/dashboard";
        }

        return "instructor-grade-submission";
    }

    @PostMapping("/submissions/{id}/grade")
    public String saveGrade(@PathVariable Long id,
                            @RequestParam Double score,
                            @RequestParam(required = false) String feedback,
                            RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== SAVE GRADE ===");
            System.out.println("Submission ID: " + id);
            System.out.println("Score: " + score);

            // First, get the course ID using native query to avoid lazy loading
            List<Object[]> results = submissionRepository.findSubmissionByIdNative(id);
            if (results.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Submission not found");
                return "redirect:/instructor/dashboard";
            }

            Object[] row = results.get(0);
            Long courseId = (Long) row[5];  // course_id is at index 5
            System.out.println("Course ID from native query: " + courseId);

            // Now update the submission using regular JPA (this works for simple update)
            Submission submission = submissionRepository.findById(id).orElse(null);
            if (submission == null) {
                redirectAttributes.addFlashAttribute("error", "Submission not found");
                return "redirect:/instructor/dashboard";
            }

            // Update the submission
            submission.setScore(BigDecimal.valueOf(score));
            submission.setFeedback(feedback);
            submission.setStatus(SubmissionStatus.GRADED);
            submissionRepository.save(submission);

            System.out.println("Grade saved successfully!");

            redirectAttributes.addFlashAttribute("message", "Grade saved successfully!");
            return "redirect:/instructor/courses/" + courseId + "/grade";

        } catch (Exception e) {
            System.err.println("ERROR saving grade: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to save grade: " + e.getMessage());
            return "redirect:/instructor/submissions/" + id + "/grade";
        }
    }

}