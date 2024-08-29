package com.paulpladziewicz.fremontmi.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;

@Service
public class EmailService {

    private final String companyName = "FremontMI";

    private final JavaMailSender mailSender;

    private SpringTemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendWelcomeEmail(String to) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            Context context = new Context();
            context.setVariable("name", "Paul");

            String html = templateEngine.process("auth/email/welcome-email", context);

            message.setFrom(new InternetAddress("no-reply@fremontmi.com", companyName));
            message.setRecipients(MimeMessage.RecipientType.TO, to);
            message.setSubject("Welcome to FremontMI.com");
            message.setContent(html, "text/html; charset=utf-8");

            mailSender.send(message);
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendForgotUsernameEmail(String email, String s) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            Context context = new Context();
            context.setVariable("username", s);

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

    public void sendResetPasswordEmail(String email, String s) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            Context context = new Context();
            context.setVariable("resetPasswordUrl", s);

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
}