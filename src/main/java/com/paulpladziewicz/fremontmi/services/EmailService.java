package com.paulpladziewicz.fremontmi.services;

import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.io.UnsupportedEncodingException;
import java.util.List;

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

    public void sendContactUsEmail(String name, String email, String contactMessage) {
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

    public void sendGroupEmail(List<String> recipients, String subject, String messageBody, String replyTo) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            message.setFrom(new InternetAddress("no-reply@fremontmi.com", companyName));
            message.setSubject("Group Message: " + subject);
            message.setContent(messageBody, "text/html; charset=utf-8");
            message.setReplyTo(new Address[]{new InternetAddress(replyTo)});

            for (String recipient : recipients) {
                message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(recipient));
                mailSender.send(message);
            }

        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }
}