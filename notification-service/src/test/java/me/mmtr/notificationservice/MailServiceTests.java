package me.mmtr.notificationservice;

import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import me.mmtr.notificationservice.model.dto.UserIdEmailDto;
import me.mmtr.notificationservice.model.dto.message.*;
import me.mmtr.notificationservice.service.MailService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.javamail.JavaMailSender;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MailServiceTests {
    private static final String FROM_EMAIL = "noreply@ms-tickets.com";
    private static final String RECIPIENT_EMAIL = "user@example.com";
    @Mock
    private JavaMailSender mailSender;
    private MailService mailService;

    @BeforeEach
    void setUp() {
        mailService = new MailService(mailSender, FROM_EMAIL);
    }

    @Nested
    class SingleEmailTests {
        private final MimeMessage mimeMessage = new MimeMessage((Session) null);

        @BeforeEach
        void setUp() {
            when(mailSender.createMimeMessage()).thenReturn(mimeMessage);
        }

        @Test
        void shouldSendUserCreatedEmail() throws MessagingException {
            mailService.sendUserCreatedMail(RECIPIENT_EMAIL);

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.captor();
            verify(mailSender).send(captor.capture());

            MimeMessage sentMessage = captor.getValue();
            assertEquals(
                    "Welcome to ms-tickets!",
                    sentMessage.getSubject()
            );
            assertEquals(
                    RECIPIENT_EMAIL,
                    sentMessage.getAllRecipients()[0].toString()
            );
        }

        @Test
        void shouldSendUserDeletedEmail() throws MessagingException {
            mailService.sendUserDeletedMail(RECIPIENT_EMAIL);

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.captor();
            verify(mailSender).send(captor.capture());

            MimeMessage sentMessage = captor.getValue();
            assertEquals(
                    "Account Deletion Confirmation - ms-tickets",
                    sentMessage.getSubject()
            );
            assertEquals(
                    RECIPIENT_EMAIL,
                    sentMessage.getAllRecipients()[0].toString()
            );
        }

        @Test
        void shouldSendOrderCreatedEmail() throws MessagingException {
            OrderCreatedMessage message = new OrderCreatedMessage(
                    1L,
                    1L,
                    "event",
                    50
            );

            mailService.sendOrderCreatedMail(
                    RECIPIENT_EMAIL,
                    message
            );


            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.captor();
            verify(mailSender).send(captor.capture());

            MimeMessage sentMessage = captor.getValue();
            assertEquals(
                    "Order Confirmation #" + message.id() + " - ms-tickets",
                    sentMessage.getSubject()
            );
            assertEquals(
                    RECIPIENT_EMAIL,
                    sentMessage.getAllRecipients()[0].toString()
            );
        }

        @Test
        void shouldSendOrderDeletedEmail() throws MessagingException {
            OrderDeletedMessage message = new OrderDeletedMessage(
                    1L,
                    1L,
                    "event",
                    50
            );

            mailService.sendOrderDeletedMail(
                    RECIPIENT_EMAIL,
                    message
            );


            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.captor();
            verify(mailSender).send(captor.capture());

            MimeMessage sentMessage = captor.getValue();
            assertEquals(
                    "Order Cancellation #" + message.id() + " - ms-tickets",
                    sentMessage.getSubject()
            );
            assertEquals(
                    RECIPIENT_EMAIL,
                    sentMessage.getAllRecipients()[0].toString()
            );
        }
    }

    @Nested
    class MultipleEmailTests {
        private final UserIdEmailDto userIdEmailDto1 =
                new UserIdEmailDto(1L, "user1@example.com");
        private final UserIdEmailDto userIdEmailDto2 =
                new UserIdEmailDto(2L, "user2@example.com");

        List<UserIdEmailDto> recipients =
                List.of(userIdEmailDto1, userIdEmailDto2);

        private final MimeMessage mimeMessage1 =
                new MimeMessage((Session) null);
        private final MimeMessage mimeMessage2 =
                new MimeMessage((Session) null);

        @BeforeEach
        void setUp() {
            when(mailSender.createMimeMessage())
                    .thenReturn(mimeMessage1)
                    .thenReturn(mimeMessage2);
        }

        @Test
        void shouldSendEmailToAllUsersOnEventUpdate() throws MessagingException {
            EventUpdatedMessage message = new EventUpdatedMessage(
                    "Old Event",
                    BigDecimal.valueOf(60.00),
                    OffsetDateTime.now().plusDays(1),
                    OffsetDateTime.now().minusDays(2),
                    100,
                    "New Event",
                    BigDecimal.valueOf(50.00),
                    OffsetDateTime.now().plusDays(3),
                    OffsetDateTime.now().plusDays(4),
                    80,
                    List.of(userIdEmailDto1.userId(), userIdEmailDto2.userId())
            );

            mailService.sendEventUpdatedMail(message, recipients);

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.captor();
            verify(
                    mailSender,
                    times(2)
            ).send(captor.capture());

            List<MimeMessage> messages = captor.getAllValues();

            assertEquals("Event Update: " + message.newName() + " - ms-tickets", messages.get(0).getSubject());
            assertEquals(
                    "user1@example.com",
                    messages.get(0).getAllRecipients()[0].toString()
            );

            assertEquals("Event Update: " + message.newName() + " - ms-tickets", messages.get(1).getSubject());
            assertEquals(
                    "user2@example.com",
                    messages.get(1).getAllRecipients()[0].toString()
            );
        }

        @Test
        void shouldSendEmailToAllUsersOnEventStatusChanged() throws MessagingException {
            EventStatusChangedMessage message = new EventStatusChangedMessage(
                    "Event",
                    List.of(userIdEmailDto1.userId(), userIdEmailDto2.userId())
            );

            mailService.sendEventStatusChangedMail(message, recipients);

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.captor();
            verify(mailSender, times(2))
                    .send(captor.capture());

            List<MimeMessage> sentMessages = captor.getAllValues();

            assertEquals("Important update regarding your event: " + message.name(), sentMessages.get(0).getSubject());
            assertEquals(
                    "user1@example.com",
                    sentMessages.get(0).getAllRecipients()[0].toString()
            );

            assertEquals(
                    "Important update regarding your event: " + message.name(),
                    sentMessages.get(1).getSubject()
            );
            assertEquals(
                    "user2@example.com",
                    sentMessages.get(1).getAllRecipients()[0].toString()
            );
        }

        @Test
        void shouldSendEmailToAllUsersOnEventDeleted() throws MessagingException {
            EventDeletedMessage message = new EventDeletedMessage(
                    "Event",
                    List.of(userIdEmailDto1.userId(), userIdEmailDto2.userId())
            );

            mailService.sendEventDeletedMail(message, recipients);

            ArgumentCaptor<MimeMessage> captor = ArgumentCaptor.captor();
            verify(mailSender, times(2))
                    .send(captor.capture());

            List<MimeMessage> sentMessages = captor.getAllValues();

            assertEquals(
                    "DELETED: Event " + message.name(),
                    sentMessages.get(0).getSubject());
            assertEquals(
                    "user1@example.com",
                    sentMessages.get(0).getAllRecipients()[0].toString()
            );

            assertEquals(
                    "DELETED: Event " + message.name(),
                    sentMessages.get(1).getSubject()
            );
            assertEquals(
                    "user2@example.com",
                    sentMessages.get(1).getAllRecipients()[0].toString()
            );
        }
    }
}