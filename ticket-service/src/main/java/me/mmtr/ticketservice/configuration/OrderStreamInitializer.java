package me.mmtr.ticketservice.configuration;

import jakarta.annotation.PostConstruct;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

@Component
public class OrderStreamInitializer {
    private static final String STREAM_KEY = "order-requests";
    private static final String GROUP_NAME = "order-processors";

    private final StringRedisTemplate redisTemplate;

    public OrderStreamInitializer(StringRedisTemplate stringRedisTemplate) {
        this.redisTemplate = stringRedisTemplate;
    }

    @PostConstruct
    public void init() {
        try {
            redisTemplate
                    .opsForStream()
                    .createGroup(
                            STREAM_KEY,
                            ReadOffset.from("0"),
                            GROUP_NAME
                    );
        } catch (Exception _) {}
    }
}
