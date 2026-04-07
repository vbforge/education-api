package com.vbforge.educationapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;

    // ── Enrollment confirmation ──────────────────────────────────

    @Async
    public void sendEnrollmentConfirmation(String toEmail,
                                           String studentName,
                                           String courseName) {
        String subject = "You're enrolled in: " + courseName;
        String body = """
                Hi %s,

                You have successfully enrolled in "%s".
                Good luck with your studies!

                — EduForge Team
                """.formatted(studentName, courseName);

        sendEmail(toEmail, subject, body);
    }

    // ── Assignment due date reminder ─────────────────────────────

    @Async
    public void sendDueDateReminder(String toEmail,
                                    String studentName,
                                    String assignmentTitle,
                                    String courseName,
                                    String dueDate) {
        String subject = "Reminder: \"" + assignmentTitle + "\" is due soon";
        String body = """
                Hi %s,

                This is a reminder that your assignment "%s" in "%s" is due on %s.

                Don't forget to submit on time!

                — EduForge Team
                """.formatted(studentName, assignmentTitle, courseName, dueDate);

        sendEmail(toEmail, subject, body);
    }

    // ── Grade posted notification ────────────────────────────────

    @Async
    public void sendGradeNotification(String toEmail,
                                      String studentName,
                                      String assignmentTitle,
                                      String score) {
        String subject = "Grade posted for: " + assignmentTitle;
        String body = """
                Hi %s,

                Your submission for "%s" has been graded.
                Score: %s

                Log in to EduForge to view instructor feedback.

                — EduForge Team
                """.formatted(studentName, assignmentTitle, score);

        sendEmail(toEmail, subject, body);
    }

    // ── Course completion ────────────────────────────────────────

    @Async
    public void sendCourseCompletionNotification(String toEmail,
                                                 String studentName,
                                                 String courseName,
                                                 String finalGrade) {
        String subject = "Congratulations! You completed: " + courseName;
        String body = """
                Hi %s,

                Congratulations on completing "%s"!
                Your final grade is: %s

                — EduForge Team
                """.formatted(studentName, courseName, finalGrade);

        sendEmail(toEmail, subject, body);
    }

    // ── Private helper ───────────────────────────────────────────

    private void sendEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            message.setFrom("noreply@eduforge.com");
            mailSender.send(message);
            log.info("Email sent to {}: {}", to, subject);
        } catch (Exception e) {
            // never crash the main flow because of email failure
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}