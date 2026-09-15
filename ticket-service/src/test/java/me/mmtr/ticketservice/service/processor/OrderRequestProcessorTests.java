package me.mmtr.ticketservice.service.processor;

import me.mmtr.ticketservice.model.dto.messaging.OrderRequestMessage;
import me.mmtr.ticketservice.model.exception.OrderNotFoundException;
import me.mmtr.ticketservice.service.OrderProcessingService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class OrderRequestProcessorTests {
    private static final String STREAM_KEY = "order-requests";
    private static final String CONSUMER_GROUP_ = "order-processors";
    @Mock
    private StreamOperations<String, Object, Object> streamOperations;
    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private OrderProcessingService orderProcessingService;

    @InjectMocks
    private OrderRequestProcessor orderRequestProcessor;

    @BeforeEach
    public void setUp() {
        when(redisTemplate.opsForStream()).thenReturn(streamOperations);
    }

    @Test
    void shouldReturnWhenNoMessagesToProcess() {
        when(streamOperations.read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                any(StreamOffset.class))
        ).thenReturn(null);

        orderRequestProcessor.poll();

        verifyNoInteractions(orderProcessingService, objectMapper);
        verify(streamOperations, never()).acknowledge(
                anyString(),
                anyString(),
                any(RecordId.class)
        );
    }

    @Test
    void shouldProcessOrderAndAcknowledgeWhenMessageIsValid() {
        String payload = "payload";
        RecordId recordId = RecordId.of("13543654-0");
        OrderRequestMessage request = new OrderRequestMessage(
                10L,
                100L,
                1L,
                2L
        );

        MapRecord<String, String, String> message = MapRecord.create(
                STREAM_KEY,
                Map.of("payload", payload)
        ).withId(recordId);

        when(streamOperations.read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                any(StreamOffset.class))
        ).thenReturn(List.of(message));

        when(objectMapper.readValue(anyString(), eq(OrderRequestMessage.class)))
                .thenReturn(request);

        orderRequestProcessor.poll();

        verify(objectMapper).readValue(payload, OrderRequestMessage.class);
        verify(orderProcessingService).processOrder(request);
        verify(streamOperations).acknowledge(
                STREAM_KEY,
                CONSUMER_GROUP_,
                recordId
        );
    }

    @Test
    void shouldNotAcknowledgeWhenProcessingFails() {
        String payload = "payload";
        RecordId recordId = RecordId.of("13543654-0");
        OrderRequestMessage request = new OrderRequestMessage(
                10L,
                100L,
                1L,
                2L
        );

        MapRecord<String, String, String> message = MapRecord.create(
                STREAM_KEY,
                Map.of("payload", payload)
        ).withId(recordId);

        when(streamOperations.read(
                any(Consumer.class),
                any(StreamReadOptions.class),
                any(StreamOffset.class))
        ).thenReturn(List.of(message));

        when(objectMapper.readValue(anyString(), eq(OrderRequestMessage.class)))
                .thenReturn(request);

        doThrow(new OrderNotFoundException(10L))
                .when(orderProcessingService)
                .processOrder(request);

        orderRequestProcessor.poll();

        verify(streamOperations, never()).acknowledge(
                anyString(),
                anyString(),
                any(RecordId.class)
        );
    }
}
