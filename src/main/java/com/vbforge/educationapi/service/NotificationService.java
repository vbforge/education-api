package com.vbforge.educationapi.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final JavaMailSender mailSender;
    
    @Value("${spring.mail.username:}")
    private String fromEmail;


    @Async
    public void sendEnrollmentConfirmation(String toEmail, String studentName, String courseName) {
        String subject = "🎓 Welcome to " + courseName + "!";
        String body = String.format("""
            Hello %s,
            
            You have successfully enrolled in "%s".
            
            Course access:
            • Go to your dashboard to access course materials
            • Complete assignments before due dates
            • Track your progress
            
            Good luck with your studies!
            
            — EduFlow Team
            """, studentName, courseName);
        
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendDueDateReminder(String toEmail, String studentName, String assignmentTitle, 
                                    String courseName, String dueDate) {
        String subject = "⏰ Reminder: " + assignmentTitle + " is due soon";
        String body = String.format("""
            Hello %s,
            
            This is a reminder that your assignment "%s" in "%s" is due on %s.
            
            Don't forget to submit before the deadline!
            
            — EduFlow Team
            """, studentName, assignmentTitle, courseName, dueDate);
        
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendGradeNotification(String toEmail, String studentName, String assignmentTitle, String score) {
        String subject = "📊 Your assignment has been graded: " + assignmentTitle;
        String body = String.format("""
            Hello %s,
            
            Your submission for "%s" has been graded.
            
            Score: %s
            
            Log in to EduFlow to view detailed feedback.
            
            — EduFlow Team
            """, studentName, assignmentTitle, score);
        
        sendEmail(toEmail, subject, body);
    }

    @Async
    public void sendCourseCompletionNotification(String toEmail, String studentName, 
                                                  String courseName, String finalGrade) {
        String subject = "🎉 Congratulations! You completed " + courseName;
        String body = String.format("""
            Hello %s,
            
            Congratulations on completing "%s"!
            
            Your final grade: %s
            
            Certificate will be available in your profile.
            
            — EduFlow Team
            """, studentName, courseName, finalGrade);
        
        sendEmail(toEmail, subject, body);
    }

    private void sendEmail(String to, String subject, String body) {
        if (to == null || to.isEmpty()) {
            log.warn("Cannot send email - no recipient address");
            return;
        }
        
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            
            if (fromEmail != null && !fromEmail.isEmpty()) {
                message.setFrom(fromEmail);
            } else {
                message.setFrom("noreply@eduflow.com");
            }
            
            mailSender.send(message);
            log.info("✅ Email sent to {}: {}", to, subject);
            
        } catch (MailException e) {
            log.error("❌ Failed to send email to {}: {}", to, e.getMessage());
            // Don't throw exception - email failure shouldn't break the main flow
        }
    }

    @Async
    public void sendPasswordResetEmail(String toEmail, String subject, String body) {
        sendEmail(toEmail, subject, body);
    }
}