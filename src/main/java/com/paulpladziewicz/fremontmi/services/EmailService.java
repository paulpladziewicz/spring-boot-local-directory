package com.paulpladziewicz.fremontmi.services;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

@Service
public class EmailService {

    private final JavaMailSender mailSender;

    private SpringTemplateEngine templateEngine;

    public EmailService(JavaMailSender mailSender, SpringTemplateEngine templateEngine) {
        this.mailSender = mailSender;
        this.templateEngine = templateEngine;
    }

    public void sendSimpleMessage(String to, String subject, String text) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            Context context = new Context();
            context.setVariable("name", "Paul");

            String html = templateEngine.process("email/reset-password", context);

            message.setFrom("no-reply@fremontmi.com");
            message.setRecipients(MimeMessage.RecipientType.TO, to);
            message.setSubject(subject);
            message.setContent(html, "text/html; charset=utf-8");

            mailSender.send(message);
        } catch (MessagingException | MailException e) {
            throw new RuntimeException(e);
        }
    }
}