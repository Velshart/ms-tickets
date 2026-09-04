package me.mmtr.notificationservice.service.listener;

import me.mmtr.notificationservice.model.dto.UserIdEmailDto;
import me.mmtr.notificationservice.model.dto.message.OrderCreatedMessage;
import me.mmtr.notificationservice.model.dto.message.OrderDeletedMessage;
import me.mmtr.notificationservice.service.MailService;
import me.mmtr.notificationservice.service.UserIdEmailService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class OrderEventListener {
    private final MailService mailService;
    private final UserIdEmailService userIdEmailService;

    public OrderEventListener(
            MailService mailService,
            UserIdEmailService userIdEmailService
    ) {
        this.mailService = mailService;
        this.userIdEmailService = userIdEmailService;
    }

    @RabbitListener(queues = "order.created.queue")
    public void handleOrderCreated(OrderCreatedMessage message) {
        UserIdEmailDto userIdEmailDto =
                userIdEmailService.findByUserId(message.userId());
        mailService.sendOrderCreatedMail(userIdEmailDto.email(), message);
    }

    @RabbitListener(queues = "order.deleted.queue")
    public void handleOrderDeleted(OrderDeletedMessage message) {
        UserIdEmailDto userIdEmailDto =
                userIdEmailService.findByUserId(message.userId());
        mailService.sendOrderDeletedMail(userIdEmailDto.email(), message);
    }
}
