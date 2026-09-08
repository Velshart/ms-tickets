package me.mmtr.notificationservice.service;

import jakarta.mail.MessagingException;
import me.mmtr.notificationservice.model.dto.UserIdEmailDto;
import me.mmtr.notificationservice.model.dto.message.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class MailService {

    private final JavaMailSender mailSender;
    private final String fromEmail;

    public MailService(
            JavaMailSender mailSender,
            @Value("${spring.mail.username}") String fromEmail
    ) {
        this.mailSender = mailSender;
        this.fromEmail = fromEmail;
    }

    public void sendUserCreatedMail(String email) {
        String subject = "Welcome to ms-tickets!";
        String content = """
                <h2>Welcome aboard!</h2>
                <p>Thank you for creating an account with <strong>ms-tickets</strong>.</p>
                <p>You can now browse available events and book your tickets online instantly.</p>
                <br>
                <p>Best regards,<br>ms-tickets</p>
                """;
        sendEmail(email, subject, content);
    }

    public void sendUserDeletedMail(String email) {
        String subject = "Account Deletion Confirmation - ms-tickets";
        String content = """
                <h2>Account Deleted</h2>
                <p>Your account on <strong>ms-tickets</strong> has been successfully deleted.</p>
                <p>If this was a mistake, please contact the administrator.</p>
                <br>
                <p>Best regards,<br>ms-tickets</p>
                """;
        sendEmail(email, subject, content);
    }

    public void sendOrderCreatedMail(String email, OrderCreatedMessage message) {
        String subject = "Order Confirmation #" + message.id() + " - ms-tickets";
        String content = """
                <h2>Order Confirmation</h2>
                <p>Thank you for your order!</p>
                <p><strong>Order ID:</strong> %d</p>
                <p><strong>Event:</strong> %s</p>
                <p><strong>Tickets:</strong> %d</p>
                <br>
                <p>Best regards,<br>ms-tickets</p>
                """.formatted(message.id(), message.eventName(), message.ticketQuantity());

        sendEmail(email, subject, content);
    }

    public void sendOrderDeletedMail(String email, OrderDeletedMessage message) {
        String subject = "Order Cancellation #" + message.id() + " - ms-tickets";
        String content = """
                <h2>Order Cancelled</h2>
                <p>Your order has been successfully cancelled.</p>
                <p><strong>Order ID:</strong> %d</p>
                <p><strong>Event:</strong> %s</p>
                <p><strong>Tickets:</strong> %d</p>
                <br>
                <p>Best regards,<br>ms-tickets</p>
                """.formatted(message.id(), message.eventName(), message.ticketQuantity());

        sendEmail(email, subject, content);
    }

    public void sendEventUpdatedMail(EventUpdatedMessage message, List<UserIdEmailDto> userIdEmailDtos) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss Z");

        String subject = "Event Update: " + message.newName() + " - ms-tickets";
        String content = """
                <h2>Event Details Updated</h2>
                <p>Details for the event <strong>%s</strong> have been updated.</p>
                
                <h3>Summary of changes:</h3>
                <ul>
                <li><strong>Name:</strong> %s &rarr; <strong>%s</strong></li>
                <li><strong>Price:</strong> $%.2f &rarr; <strong>$%.2f</strong></li>
                <li><strong>Start Date:</strong> %s &rarr; <strong>%s</strong></li>
                <li><strong>End Date:</strong> %s &rarr; <strong>%s</strong></li>
                <li><strong>Available Tickets:</strong> %d &rarr; <strong>%d</strong></li>
                </ul>
                <br>
                <p>Best regards,<br>ms-tickets</p>
                """.formatted(
                message.oldName(),
                message.oldName(), message.newName(),
                message.oldPrice(), message.newPrice(),
                message.oldStartDate().format(formatter), message.newStartDate().format(formatter),
                message.oldEndDate().format(formatter), message.newEndDate().format(formatter),
                message.oldAvailableTickets(), message.newAvailableTickets()
        );

        for (UserIdEmailDto user : userIdEmailDtos) {
            sendEmail(user.email(), subject, content);
        }
    }

    public void sendEventStatusChangedMail(EventStatusChangedMessage message, List<UserIdEmailDto> userIdEmailDtos) {
        String subject = "Important update regarding your event: " + message.name();
        String content = """
                <h2>Event Status Changed</h2>
                <p>There has been a change in status for the event <strong>%s</strong> you booked tickets for.</p>
                <p>Please log in to your account to check the latest updates regarding your reservation.</p>
                <br>
                <p>Best regards,<br>ms-tickets</p>
                """.formatted(message.name());

        for (UserIdEmailDto recipient : userIdEmailDtos) {
            sendEmail(recipient.email(), subject, content);
        }
    }

    public void sendEventDeletedMail(EventDeletedMessage message, List<UserIdEmailDto> userIdEmailDtos) {
        String subject = "DELETED: Event " + message.name();
        String content = """
                <h2>Event Deleted</h2>
                <p>We regret to inform you that the event <strong>%s</strong> has been deleted.</p>
                <p>If you had active orders, a refund or system update will be processed automatically.</p>
                <br>
                <p>Best regards,<br>ms-tickets</p>
                """.formatted(message.name());

        for (UserIdEmailDto recipient : userIdEmailDtos) {
            sendEmail(recipient.email(), subject, content);
        }
    }

    private void sendEmail(String to, String subject, String htmlContent) {
        try {
            var mimeMessage = mailSender.createMimeMessage();
            var message = new MimeMessageHelper(
                    mimeMessage,
                    true,
                    StandardCharsets.UTF_8.name()
            );

            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject(subject);
            message.setText(htmlContent, true);

            mailSender.send(mimeMessage);
        } catch (MessagingException e) {
            throw new RuntimeException("Failed to send email to " + to, e);
        }
    }
}