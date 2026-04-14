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

            // Get courses taught by this instructor
            List<Course> allCourses = courseRepository.findAll();
            List<Course> myCourses = allCourses.stream()
                    .filter(c -> c.getInstructor() != null && instructorEmail.equalsIgnoreCase(c.getInstructor().trim()))
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
                int pending = submissionRepository.findByCourseIdAndStatus(course.getId(), SubmissionStatus.SUBMITTED).size();

                courseMap.put("enrollmentCount", enrollmentCount);
                courseMap.put("moduleCount", moduleCount);
                courseMap.put("pendingSubmissions", pending);

                totalStudents += enrollmentCount;
                pendingGrading += pending;
                courseList.add(courseMap);
            }

            // Calculate average completion
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

            // Get pending submissions for the dashboard using native query
            List<Map<String, Object>> pendingSubmissions = new ArrayList<>();
            for (Course course : myCourses) {
                // Use native query to get pending submissions for this course
                List<Object[]> results = submissionRepository.findPendingSubmissionsByCourseIdNative(course.getId());
                for (Object[] row : results) {
                    Map<String, Object> subMap = new HashMap<>();
                    subMap.put("id", row[0]);
                    subMap.put("assignmentTitle", row[1]);
                    subMap.put("studentName", row[2]);
                    subMap.put("courseName", course.getName());
                    subMap.put("submittedAt", row[3]);
                    pendingSubmissions.add(subMap);
                    System.out.println("Added pending submission: " + row[1] + " - " + row[2]);
                }
            }

            // Limit to 5 most recent
            if (pendingSubmissions.size() > 5) {
                pendingSubmissions = pendingSubmissions.subList(0, 5);
            }

            System.out.println("Final stats - Courses: " + myCourses.size() + ", Students: " + totalStudents +
                    ", Pending: " + pendingGrading + ", Dashboard pending list: " + pendingSubmissions.size());

            model.addAttribute("courseCount", myCourses.size());
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("pendingGrading", pendingGrading);
            model.addAttribute("avgCompletion", Math.round(avgCompletion));
            model.addAttribute("courses", courseList);
            model.addAttribute("pendingSubmissions", pendingSubmissions);
            model.addAttribute("title", "Instructor Dashboard");

        } catch (Exception e) {
            System.err.println("ERROR in instructor dashboard: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("courseCount", 0);
            model.addAttribute("totalStudents", 0);
            model.addAttribute("pendingGrading", 0);
            model.addAttribute("avgCompletion", 0);
            model.addAttribute("courses", new ArrayList<>());
            model.addAttribute("pendingSubmissions", new ArrayList<>());
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
            System.out.println("=== MY COURSES DEBUG ===");
            System.out.println("Instructor email: " + instructorEmail);

            List<Course> allCourses = courseRepository.findAll();
            System.out.println("Total courses in DB: " + allCourses.size());

            for (Course c : allCourses) {
                System.out.println("  Course: '" + c.getName() + "', Instructor: '" + c.getInstructor() + "'");
            }

            List<Course> myCourses = allCourses.stream()
                    .filter(c -> c.getInstructor() != null && instructorEmail.equalsIgnoreCase(c.getInstructor().trim()))
                    .collect(Collectors.toList());

            System.out.println("Courses matching instructor: " + myCourses.size());

            List<Map<String, Object>> courseList = new ArrayList<>();
            for (Course course : myCourses) {
                Map<String, Object> courseMap = new HashMap<>();
                courseMap.put("id", course.getId());
                courseMap.put("name", course.getName());
                courseMap.put("enrollmentCount", courseRepository.countEnrollmentsByCourseId(course.getId()));
                courseMap.put("moduleCount", courseRepository.countModulesByCourseId(course.getId()));
                courseMap.put("pendingSubmissions", submissionRepository.findByCourseIdAndStatus(course.getId(), SubmissionStatus.SUBMITTED).size());
                courseList.add(courseMap);
                System.out.println("  Added course: " + course.getName());
            }

            model.addAttribute("courses", courseList);
            model.addAttribute("title", "My Courses");

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
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

            // Make sure to keep the instructor
            Course existingCourse = courseRepository.findById(id).orElse(null);
            if (existingCourse != null) {
                dto.setInstructor(existingCourse.getInstructor());
            }

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

    @GetMapping("/modules/{id}/edit")
    public String editModule(@PathVariable Long id, Model model) {
        try {
            ModuleResponseDto module = moduleService.findById(id);
            System.out.println("Editing module: " + module.getTitle() + " for course: " + module.getCourseId());
            model.addAttribute("module", module);
            model.addAttribute("courseId", module.getCourseId());
            model.addAttribute("title", "Edit Module - " + module.getTitle());
            return "instructor-module-edit";
        } catch (Exception e) {
            System.err.println("Error editing module: " + e.getMessage());
            return "redirect:/instructor/courses";
        }
    }

    @PostMapping("/modules/{id}/edit")
    public String updateModule(@PathVariable Long id,
                               @RequestParam String title,
                               @RequestParam(required = false) String content,
                               @RequestParam(defaultValue = "0") int orderIndex,
                               RedirectAttributes redirectAttributes) {
        try {
            System.out.println("Updating module ID: " + id);
            System.out.println("New title: " + title);

            ModuleRequestDto dto = new ModuleRequestDto();
            dto.setTitle(title);
            dto.setContent(content);
            dto.setOrderIndex(orderIndex);

            ModuleResponseDto module = moduleService.update(id, dto);
            System.out.println("Module updated successfully!");

            redirectAttributes.addFlashAttribute("message", "Module updated successfully!");
            return "redirect:/instructor/courses/" + module.getCourseId() + "/modules";
        } catch (Exception e) {
            System.err.println("Error updating module: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to update module: " + e.getMessage());
            return "redirect:/instructor/modules/" + id + "/edit";
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

    @GetMapping("/assignments/{id}/edit")
    public String editAssignment(@PathVariable Long id, Model model) {
        try {
            AssignmentResponseDto assignment = assignmentService.findById(id);
            model.addAttribute("assignment", assignment);
            model.addAttribute("moduleId", assignment.getModuleId());
            model.addAttribute("title", "Edit Assignment - " + assignment.getTitle());
            return "instructor-assignment-edit";
        } catch (Exception e) {
            return "redirect:/instructor/courses";
        }
    }

    @PostMapping("/assignments/{id}/edit")
    public String updateAssignment(@PathVariable Long id,
                                   @RequestParam String title,
                                   @RequestParam(required = false) String description,
                                   @RequestParam(defaultValue = "100") int pointsPossible,
                                   @RequestParam(required = false) String dueDate,
                                   RedirectAttributes redirectAttributes) {
        try {
            System.out.println("=== UPDATE ASSIGNMENT ===");
            System.out.println("Assignment ID: " + id);
            System.out.println("Title: " + title);
            System.out.println("Points: " + pointsPossible);
            System.out.println("Due Date: " + dueDate);

            AssignmentRequestDto dto = new AssignmentRequestDto();
            dto.setTitle(title);
            dto.setDescription(description);
            dto.setPointsPossible(pointsPossible);
            if (dueDate != null && !dueDate.isEmpty()) {
                dto.setDueDate(java.time.LocalDateTime.parse(dueDate + "T23:59:59"));
            }

            AssignmentResponseDto assignment = assignmentService.update(id, dto);
            System.out.println("Assignment updated! Module ID: " + assignment.getModuleId());

            redirectAttributes.addFlashAttribute("message", "Assignment updated successfully!");
            return "redirect:/instructor/modules/" + assignment.getModuleId() + "/assignments";
        } catch (Exception e) {
            System.err.println("Error updating assignment: " + e.getMessage());
            e.printStackTrace();
            redirectAttributes.addFlashAttribute("error", "Failed to update assignment: " + e.getMessage());
            return "redirect:/instructor/assignments/" + id + "/edit";
        }
    }

    @PostMapping("/assignments/{id}/delete")
    public String deleteAssignment(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            AssignmentResponseDto assignment = assignmentService.findById(id);
            Long moduleId = assignment.getModuleId();
            assignmentService.delete(id);
            redirectAttributes.addFlashAttribute("message", "Assignment deleted successfully!");
            return "redirect:/instructor/modules/" + moduleId + "/assignments";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Failed to delete assignment: " + e.getMessage());
            return "redirect:/instructor/courses";
        }
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

    @GetMapping("/grading")
    public String allPendingSubmissions(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            String instructorEmail = userDetails.getUsername();
            System.out.println("=== ALL PENDING SUBMISSIONS ===");

            // Get courses taught by this instructor
            List<Course> allCourses = courseRepository.findAll();
            List<Course> myCourses = allCourses.stream()
                    .filter(c -> c.getInstructor() != null && instructorEmail.equalsIgnoreCase(c.getInstructor().trim()))
                    .collect(Collectors.toList());

            // Get all pending submissions from instructor's courses
            List<Map<String, Object>> allPending = new ArrayList<>();
            for (Course course : myCourses) {
                List<Object[]> results = submissionRepository.findPendingSubmissionsByCourseIdNative(course.getId());
                for (Object[] row : results) {
                    Map<String, Object> subMap = new HashMap<>();
                    subMap.put("id", row[0]);
                    subMap.put("assignmentTitle", row[1]);
                    subMap.put("studentName", row[2]);
                    subMap.put("courseName", course.getName());
                    subMap.put("submittedAt", row[3]);
                    allPending.add(subMap);
                }
            }

            System.out.println("Total pending submissions: " + allPending.size());

            model.addAttribute("submissions", allPending);
            model.addAttribute("title", "All Pending Submissions");

        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("submissions", new ArrayList<>());
        }

        return "instructor-grading-all";
    }

    @GetMapping("/courses/{courseId}/progress")
    public String courseProgress(@PathVariable Long courseId, Model model) {
        try {
            System.out.println("=== COURSE PROGRESS ===");
            System.out.println("Course ID: " + courseId);

            // Get course using regular JPA
            Course course = courseRepository.findById(courseId).orElse(null);
            if (course == null) {
                System.out.println("Course not found!");
                model.addAttribute("error", "Course not found");
                return "redirect:/instructor/courses";
            }

            System.out.println("Course: " + course.getName());

            // Get enrollments with native query to get student data
            List<Object[]> enrollmentResults = enrollmentRepository.findEnrollmentsWithStudentByCourseIdNative(courseId);
            System.out.println("Enrollments found: " + enrollmentResults.size());

            List<Map<String, Object>> students = new ArrayList<>();
            double totalProgress = 0;
            double totalGrade = 0;
            int gradedCount = 0;
            long completedCount = 0;

            for (Object[] row : enrollmentResults) {
                Map<String, Object> studentMap = new HashMap<>();
                studentMap.put("id", row[0]);  // student_id
                studentMap.put("name", row[1]); // student_name
                studentMap.put("progressPct", row[2]); // progress_pct
                studentMap.put("grade", row[3]); // grade
                studentMap.put("status", row[4]); // status
                studentMap.put("enrolledAt", row[5]); // enrolled_at
                students.add(studentMap);

                double progress = ((Number) row[2]).doubleValue();
                totalProgress += progress;

                if (row[3] != null) {
                    double grade = ((Number) row[3]).doubleValue();
                    totalGrade += grade;
                    gradedCount++;
                }

                String status = (String) row[4];
                if ("COMPLETED".equals(status)) {
                    completedCount++;
                }

                System.out.println("  Student: " + row[1] + ", Progress: " + progress + "%, Grade: " + row[3]);
            }

            double avgProgress = enrollmentResults.size() > 0 ? totalProgress / enrollmentResults.size() : 0;
            double avgGrade = gradedCount > 0 ? totalGrade / gradedCount : 0;

            model.addAttribute("course", course);
            model.addAttribute("students", students);
            model.addAttribute("totalStudents", enrollmentResults.size());
            model.addAttribute("avgProgress", Math.round(avgProgress));
            model.addAttribute("avgGrade", Math.round(avgGrade));
            model.addAttribute("completedCount", completedCount);
            model.addAttribute("title", "Student Progress - " + course.getName());

        } catch (Exception e) {
            System.err.println("Error in courseProgress: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("students", new ArrayList<>());
        }

        return "instructor-course-progress";
    }

    @GetMapping("/students/{id}/progress")
    public String studentProgress(@PathVariable Long id,
                                  @AuthenticationPrincipal UserDetails userDetails,
                                  Model model) {
        if (userDetails == null) {
            return "redirect:/login";
        }

        try {
            Student student = studentService.getStudentOrThrow(id);

            List<Object[]> submissionResults = submissionRepository.findStudentSubmissionsWithDetailsNative(id);

            List<Map<String, Object>> assignments = new ArrayList<>();
            for (Object[] row : submissionResults) {
                Map<String, Object> assignmentMap = new HashMap<>();
                assignmentMap.put("title", row[0]);
                assignmentMap.put("dueDate", row[1]);
                assignmentMap.put("pointsPossible", row[2]);
                assignmentMap.put("score", row[3]);
                assignmentMap.put("status", row[4]);
                assignmentMap.put("filePath", row[5]);  // Add file path
                assignments.add(assignmentMap);
            }

            Map<String, Object> studentMap = new HashMap<>();
            studentMap.put("id", student.getId());
            studentMap.put("name", student.getName());
            studentMap.put("email", student.getEmail());
            studentMap.put("enrolledAt", student.getCreatedAt());
            studentMap.put("progressPct", 0);
            studentMap.put("grade", null);

            model.addAttribute("student", studentMap);
            model.addAttribute("assignments", assignments);
            model.addAttribute("courseId", 1);
            model.addAttribute("title", "Student Progress - " + student.getName());

        } catch (Exception e) {
            System.err.println("Error in studentProgress: " + e.getMessage());
            e.printStackTrace();
            model.addAttribute("error", e.getMessage());
            model.addAttribute("assignments", new ArrayList<>());
            model.addAttribute("student", new HashMap<>());
        }

        return "instructor-student-progress";
    }
    @GetMapping("/courses/{courseId}/analytics")
    public String courseAnalytics(@PathVariable Long courseId, Model model) {
        try {
            CourseResponseDto course = courseService.findById(courseId);
            List<Enrollment> enrollments = enrollmentRepository.findByCourseId(courseId);

            int totalStudents = enrollments.size();
            double avgProgress = enrollments.stream()
                    .mapToDouble(e -> e.getProgressPct().doubleValue())
                    .average().orElse(0);
            double avgGrade = enrollments.stream()
                    .filter(e -> e.getGrade() != null)
                    .mapToDouble(e -> e.getGrade().doubleValue())
                    .average().orElse(0);
            long completedCount = enrollments.stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.COMPLETED)
                    .count();
            double completionRate = totalStudents > 0 ? (completedCount * 100.0 / totalStudents) : 0;

            // Grade distribution
            List<Map<String, Object>> gradeDistribution = Arrays.asList(
                    createGradeRange("90-100%", 90, 100, enrollments),
                    createGradeRange("75-89%", 75, 89, enrollments),
                    createGradeRange("60-74%", 60, 74, enrollments),
                    createGradeRange("Below 60%", 0, 59, enrollments)
            );

            model.addAttribute("course", course);
            model.addAttribute("totalStudents", totalStudents);
            model.addAttribute("avgProgress", Math.round(avgProgress));
            model.addAttribute("avgGrade", Math.round(avgGrade));
            model.addAttribute("completionRate", Math.round(completionRate));
            model.addAttribute("gradeDistribution", gradeDistribution);
            model.addAttribute("title", "Course Analytics - " + course.getName());

        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
        }

        return "instructor-course-analytics";
    }

    private Map<String, Object> createGradeRange(String rangeName, int min, int max, List<Enrollment> enrollments) {
        Map<String, Object> range = new HashMap<>();
        long count = enrollments.stream()
                .filter(e -> e.getGrade() != null)
                .filter(e -> e.getGrade().doubleValue() >= min && e.getGrade().doubleValue() <= max)
                .count();
        range.put("range", rangeName);
        range.put("count", count);
        range.put("percentage", enrollments.size() > 0 ? (count * 100.0 / enrollments.size()) : 0);
        return range;
    }

}