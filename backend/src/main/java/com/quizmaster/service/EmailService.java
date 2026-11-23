package com.quizmaster.service;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.util.HtmlUtils;

/**
 * Sends notification emails (registration, quiz result, password reset) using Spring Boot Starter Mail.
 * When MAIL_ENABLED is false (default) the email is written to the log instead, so the app works without SMTP.
 */
@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final ObjectProvider<JavaMailSender> mailSender;
    private final boolean enabled;
    private final String from;
    private final String frontendUrl;

    public EmailService(ObjectProvider<JavaMailSender> mailSender,
                        @Value("${app.mail.enabled:false}") boolean enabled,
                        @Value("${app.mail.from:QuizMaster <no-reply@quizmaster.app>}") String from,
                        @Value("${app.frontend-url:http://localhost:5173}") String frontendUrl) {
        this.mailSender = mailSender;
        this.enabled = enabled;
        this.from = from;
        this.frontendUrl = frontendUrl.replaceAll("/$", "");
    }

    @Async
    public void sendWelcome(String toEmail, String name) {
        send(toEmail, "Welcome to QuizMaster!",
                "<h2>Hi " + esc(name) + ",</h2>"
                        + "<p>Your QuizMaster account has been created successfully.</p>"
                        + "<p><a href=\"" + frontendUrl + "\">Start taking quizzes</a></p>");
    }

    @Async
    public void sendResult(String toEmail, String name, String quizTitle, int score, int total, double percentage,
                           boolean timedOut, Long attemptId) {
        send(toEmail, "Your result: " + quizTitle,
                "<h2>Hi " + esc(name) + ",</h2>"
                        + "<p>You scored <b>" + score + " / " + total + "</b> (" + percentage + "%) in <b>" + esc(quizTitle) + "</b>.</p>"
                        + (timedOut ? "<p>Time ran out, so your answers were submitted automatically.</p>" : "")
                        + "<p><a href=\"" + frontendUrl + "/results/" + attemptId + "\">View detailed result</a></p>");
    }

    @Async
    public void sendPasswordReset(String toEmail, String name, String token) {
        String link = frontendUrl + "/reset-password?token=" + token;
        send(toEmail, "Reset your QuizMaster password",
                "<h2>Hi " + esc(name) + ",</h2>"
                        + "<p>Click the link below to set a new password. The link is valid for 30 minutes.</p>"
                        + "<p><a href=\"" + link + "\">Reset password</a></p>"
                        + "<p>If you did not ask for this, you can ignore this email.</p>");
    }

    void send(String to, String subject, String html) {
        JavaMailSender sender = mailSender.getIfAvailable();
        if (!enabled || sender == null) {
            log.info("[email disabled] To: {} | Subject: {} | Body: {}", to, subject, html.replaceAll("<[^>]+>", " "));
            return;
        }
        try {
            MimeMessage message = sender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");
            helper.setFrom(from);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(html, true);
            sender.send(message);
            log.info("Email sent to {} ({})", to, subject);
        } catch (Exception e) {
            log.warn("Could not send email to {}: {}", to, e.getMessage());
        }
    }

    private static String esc(String s) { return s == null ? "" : HtmlUtils.htmlEscape(s); }
}
