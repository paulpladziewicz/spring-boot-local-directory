package com.paulpladziewicz.fremontmi.services;

import com.stripe.model.Dispute;
import jakarta.mail.Address;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

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
    public void sendWelcomeEmailAsync(String email, String confirmationToken) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            Context context = new Context();
            context.setVariable("confirmationLink", "https://fremontmi.com/confirm?token=" + confirmationToken);

            String html = templateEngine.process("auth/email/welcome-email", context);

            message.setFrom(new InternetAddress("no-reply@fremontmi.com", companyName));
            message.setRecipients(MimeMessage.RecipientType.TO, email);
            message.setSubject("Welcome to FremontMI.com");
            message.setContent(html, "text/html; charset=utf-8");

            mailSender.send(message);
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            logger.error("Failed to send welcome email to {}", email, e);
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
            logger.error("Failed to send forgot username email to {}", email, e);
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
            logger.error("Failed to send reset password email to {}", email, e);
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
            logger.error("Failed to send contact us email from {} {} with a message of {}.", name, email, contactMessage, e);
        }
    }

    public Boolean sendGroupEmail(List<String> recipients, String replyTo, String subject, String messageBody) {
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

            return true;
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            logger.error("Failed to send group email to {} from {} with a subject of {} and message of {}.", recipients, replyTo, subject, messageBody, e);
            return false;
        }
    }

    @Async
    public void sendDisputeCreatedEmailAsync(String email, Dispute dispute) {
        try {
            MimeMessage message = mailSender.createMimeMessage();

            // Prepare the context for the email template
            Context context = new Context();
            context.setVariable("disputeId", dispute.getId());
            context.setVariable("amountDisputed", dispute.getAmount() / 100.0); // Stripe stores amounts in cents
            context.setVariable("currency", dispute.getCurrency());
            context.setVariable("reason", dispute.getReason());

            // Generate the email content using a template
            String html = templateEngine.process("stripe/email/dispute-created", context);

            message.setFrom(new InternetAddress("no-reply@fremontmi.com", companyName));
            message.setRecipients(MimeMessage.RecipientType.TO, email);
            message.setSubject("Dispute Created - " + dispute.getId());
            message.setContent(html, "text/html; charset=utf-8");

            mailSender.send(message);
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            logger.error("Failed to send dispute created email to {}", email, e);
        }
    }

    public void sendContactBusinessEmail(String toEmail, String name, String fromEmail, String message) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();

            Context context = new Context();
            context.setVariable("name", name);
            context.setVariable("fromEmail", fromEmail);
            context.setVariable("message", message);

            String html = templateEngine.process("businesses/email/contact-business-email", context);

            mimeMessage.setFrom(new InternetAddress("no-reply@fremontmi.com", companyName));
            mimeMessage.setReplyTo(new Address[]{new InternetAddress(fromEmail)});
            mimeMessage.setRecipients(MimeMessage.RecipientType.TO, toEmail);
            mimeMessage.setSubject("Business Contact Form Submission");
            mimeMessage.setContent(html, "text/html; charset=utf-8");

            mailSender.send(mimeMessage);
        } catch (MessagingException | MailException | UnsupportedEncodingException e) {
            logger.error("Failed to send business contact email to {} from {} with message: {} and name: {}", toEmail, fromEmail, message, name, e);
        }
    }
}