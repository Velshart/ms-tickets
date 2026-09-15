package me.mmtr.ticketservice.configuration.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.io.IOException;
import java.io.PrintWriter;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class RateLimitingFilterTests {

    private static final String CLIENT_IP = "192.168.1.1";
    private static final int BUCKET_CAPACITY = 10;

    @Mock
    private HttpServletRequest request;
    @Mock
    private HttpServletResponse response;
    @Mock
    private FilterChain filterChain;
    @Mock
    private PrintWriter writer;
    @InjectMocks
    private RateLimitingFilter rateLimitingFilter;

    @BeforeEach
    public void setUp() {
        when(request.getRemoteAddr()).thenReturn(CLIENT_IP);
    }

    @Test
    void shouldAllowRequestWhenUnderLimit() throws ServletException, IOException {
        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void shouldAllowMultipleRequestsUpToCapacity() throws Exception {
        for (int i = 0; i < BUCKET_CAPACITY; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        verify(filterChain, times(BUCKET_CAPACITY)).doFilter(request, response);
        verify(response, never()).setStatus(anyInt());
    }

    @Test
    void shouldBlockRequestWhenLimitExceeded() throws Exception {
        when(response.getWriter()).thenReturn(writer);

        for (int i = 0; i < BUCKET_CAPACITY; i++) {
            rateLimitingFilter.doFilterInternal(request, response, filterChain);
        }

        rateLimitingFilter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        verify(writer).write("Too many requests. Please try again later.");

        verify(filterChain, times(BUCKET_CAPACITY)).doFilter(request, response);
    }

    @Test
    void shouldTrackSeparateBucketsPerClientIp() throws Exception {
        final String otherClientIp = "192.168.1.2";

        HttpServletRequest otherHttpRequest = mock(HttpServletRequest.class);
        when(otherHttpRequest.getRemoteAddr()).thenReturn(otherClientIp);

        for (int i = 0; i < BUCKET_CAPACITY; i++) {
            rateLimitingFilter.doFilterInternal(otherHttpRequest, response, filterChain);
        }

        rateLimitingFilter.doFilterInternal(request, response, filterChain);
        verify(filterChain, times(BUCKET_CAPACITY + 1))
                .doFilter(
                        any(),
                        eq(response)
                );
        verify(response, never()).setStatus(anyInt());
    }
}
