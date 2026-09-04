package me.mmtr.notificationservice.service.listener;

import me.mmtr.notificationservice.model.dto.message.UserCreatedMessage;
import me.mmtr.notificationservice.model.dto.message.UserDeletedMessage;
import me.mmtr.notificationservice.service.MailService;
import me.mmtr.notificationservice.service.UserIdEmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class UserEventListener {
    private final MailService mailService;
    private final UserIdEmailService userIdEmailService;

    public UserEventListener(
            MailService mailService,
            UserIdEmailService userIdEmailService
    ) {
        this.mailService = mailService;
        this.userIdEmailService = userIdEmailService;
    }

    @RabbitListener(queues = "user.created.queue")
    public void handleUserCreated(UserCreatedMessage message) {
        userIdEmailService.create(message.id(), message.email());
        mailService.sendUserCreatedMail(message.email());
    }

    @RabbitListener(queues = "user.deleted.queue")
    public void handleUserDeleted(UserDeletedMessage message) {
        userIdEmailService.deleteByUserId(message.id());
        mailService.sendUserDeletedMail(message.email());
    }
}
