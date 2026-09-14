package me.mmtr.ticketservice.service.processor;

import me.mmtr.ticketservice.model.dto.messaging.OrderRequestMessage;
import me.mmtr.ticketservice.service.OrderProcessingService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.Duration;
import java.util.UUID;

@Component
public class OrderRequestProcessor {
    private static final Logger log =
            LoggerFactory.getLogger(OrderRequestProcessor.class);
    private static final String STREAM_KEY = "order-requests";
    private static final String GROUP_NAME = "order-processors";
    private final String processorName = "processor-" + UUID.randomUUID();

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final OrderProcessingService orderProcessingService;

    public OrderRequestProcessor(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            OrderProcessingService orderProcessingService
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.orderProcessingService = orderProcessingService;
    }

    @SuppressWarnings("unchecked")
    @Scheduled(fixedDelay = 500)
    public void poll() {
        var messages = redisTemplate.opsForStream().read(
                Consumer.from(GROUP_NAME, processorName),
                StreamReadOptions.empty().count(10).block(Duration.ofMillis(200)),
                StreamOffset.create(STREAM_KEY, ReadOffset.lastConsumed())
        );

        if (messages == null) {
            return;
        }

        for (MapRecord<String, Object, Object> message : messages) {
            try {
                String payload = (String) message.getValue().get("payload");
                OrderRequestMessage request =
                        objectMapper.readValue(payload, OrderRequestMessage.class);

                orderProcessingService.processOrder(request);

                redisTemplate
                        .opsForStream()
                        .acknowledge(
                                STREAM_KEY,
                                GROUP_NAME,
                                message.getId()
                        );
            } catch (Exception e) {
                log.error("Error processing order message {}", message.getId(), e);
            }
        }
    }
}
