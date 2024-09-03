package com.paulpladziewicz.fremontmi.services;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;
import java.util.List;

@Service
public class EmailService {

    private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

    private final String companyName = "FremontMI";

    private final JavaMailSender mailSender;

    private final SpringTemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    @Async
    public void sendWelcomeEmailAsync(String to) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // needs to include a confirm account button
            Context context = new Context();

            String html = templateEngine.process("auth/email/welcome-email", context);

            message.setFrom(new InternetAddress("no-reply@fremontmi.com", companyName));
            message.setRecipients(MimeMessage.RecipientType.TO, to);
            message.setSubject("Welcome to FremontMI.com");
            message.setContent(html, "text/html; charset=utf-8");

            mailSender.send(message);
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            logger.error("Failed to send welcome email to {}", to, e);
        }
    }

    @Async
    public void sendForgotUsernameEmailAsync(String email, String username) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            Context context = new Context();
            context.setVariable("username", username);

            String html = templateEngine.process("auth/email/forgot-username", context);

            message.setFrom(new InternetAddress("no-reply@fremontmi.com", companyName));
            message.setRecipients(MimeMessage.RecipientType.TO, email);
            message.setSubject("Your Username");
            message.setContent(html, "text/html; charset=utf-8");

            mailSender.send(message);
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Async
    public void sendResetPasswordEmailAsync(String email, String resetPasswordUrl) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            Context context = new Context();
            context.setVariable("resetPasswordUrl", resetPasswordUrl);

            String html = templateEngine.process("auth/email/reset-password", context);

            message.setFrom(new InternetAddress("no-reply@fremontmi.com", companyName));
            message.setRecipients(MimeMessage.RecipientType.TO, email);
            message.setSubject("Reset Password Link");
            message.setContent(html, "text/html; charset=utf-8");

            mailSender.send(message);
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Async
    public void sendContactUsEmailAsync(String name, String email, String contactMessage) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("email", email);
            context.setVariable("message", contactMessage);

            String html = templateEngine.process("contact-us-email", context);

            message.setFrom(new InternetAddress("no-reply@fremontmi.com", companyName));
            message.setRecipients(MimeMessage.RecipientType.TO, "ppladziewicz@gmail.com");
            message.setSubject("Contact Form Submission");
            message.setContent(html, "text/html; charset=utf-8");

            mailSender.send(message);
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    @Async
    public void sendGroupEmailAsync(List<String> recipients, String replyTo, String subject, String messageBody) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            message.setFrom(new InternetAddress("no-reply@fremontmi.com", companyName));
            message.setReplyTo(new Address[]{new InternetAddress(replyTo)});
            message.setSubject("Group Message: " + subject);
            message.setContent(messageBody, "text/html; charset=utf-8");

            for (String recipient : recipients) {
                message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(recipient));
                mailSender.send(message);
            }

        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}