package me.mmtr.notificationservice.service.listener;

import me.mmtr.notificationservice.model.dto.UserIdEmailDto;
import me.mmtr.notificationservice.model.dto.message.EventDeletedMessage;
import me.mmtr.notificationservice.model.dto.message.EventStatusChangedMessage;
import me.mmtr.notificationservice.model.dto.message.EventUpdatedMessage;
import me.mmtr.notificationservice.service.MailService;
import me.mmtr.notificationservice.service.UserIdEmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class EventEventListener {
    private final MailService mailService;
    private final UserIdEmailService userIdEmailService;

    public EventEventListener(
            MailService mailService,
            UserIdEmailService userIdEmailService
    ) {
        this.mailService = mailService;
        this.userIdEmailService = userIdEmailService;
    }

    @RabbitListener(queues = "event.status.changed.queue")
    public void handleEventStatusChanged(EventStatusChangedMessage message) {
        List<UserIdEmailDto> userIdEmailDtos = userIdEmailService
                .findAllByUserIdIn(message.ordersOwnersIds());

        mailService.sendEventStatusChangedMail(message, userIdEmailDtos);
    }

    @RabbitListener(queues = "event.updated.queue")
    public void handleEventUpdated(EventUpdatedMessage message) {
        List<UserIdEmailDto> userIdEmailDtos = userIdEmailService
                .findAllByUserIdIn(message.ordersOwnersIds());

        mailService.sendEventUpdatedMail(message, userIdEmailDtos);
    }

    @RabbitListener(queues = "event.deleted.queue")
    public void handleEventDeleted(EventDeletedMessage message) {
        List<UserIdEmailDto> userIdEmailDtos = userIdEmailService
                .findAllByUserIdIn(message.ordersOwnersIds());

        mailService.sendEventDeletedMail(message, userIdEmailDtos);
    }
}
