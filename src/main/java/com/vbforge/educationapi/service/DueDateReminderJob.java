package com.vbforge.educationapi.service;

import com.vbforge.educationapi.domain.Assignment;
import com.vbforge.educationapi.domain.Enrollment;
import com.vbforge.educationapi.domain.EnrollmentStatus;
import com.vbforge.educationapi.repository.AssignmentRepository;
import com.vbforge.educationapi.repository.EnrollmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DueDateReminderJob {

    private final AssignmentRepository  assignmentRepository;
    private final EnrollmentRepository  enrollmentRepository;
    private final NotificationService   notificationService;

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm");

    // runs every day at 08:00
    @Scheduled(cron = "0 0 8 * * *")
    @Transactional(readOnly = true)
    public void sendDueDateReminders() {
        LocalDateTime now      = LocalDateTime.now();
        LocalDateTime deadline = now.plusHours(24);

        List<Assignment> upcoming = assignmentRepository.findUpcomingDue(now, deadline);
        log.info("Due date reminder job: found {} assignments due within 24h", upcoming.size());

        for (Assignment assignment : upcoming) {
            Long courseId = assignment.getModule().getCourse().getId();
            String courseName      = assignment.getModule().getCourse().getName();
            String assignmentTitle = assignment.getTitle();
            String dueDate         = assignment.getDueDate().format(FORMATTER);

            // find all active students enrolled in this course
            List<Enrollment> enrollments = enrollmentRepository
                    .findByCourseId(courseId)
                    .stream()
                    .filter(e -> e.getStatus() == EnrollmentStatus.ACTIVE)
                    .toList();

            for (Enrollment enrollment : enrollments) {
                notificationService.sendDueDateReminder(
                        enrollment.getStudent().getEmail(),
                        enrollment.getStudent().getName(),
                        assignmentTitle,
                        courseName,
                        dueDate
                );
            }
        }
    }
}