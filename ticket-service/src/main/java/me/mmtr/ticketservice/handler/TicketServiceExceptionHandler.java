package me.mmtr.ticketservice.handler;

import me.mmtr.common.web.BaseExceptionHandler;
import me.mmtr.ticketservice.model.exception.EventAlreadyRegisteredException;
import me.mmtr.ticketservice.model.exception.NotFoundException;
import me.mmtr.ticketservice.model.exception.TicketException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TicketServiceExceptionHandler extends BaseExceptionHandler {
    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFoundException(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(EventAlreadyRegisteredException.class)
    public ResponseEntity<ErrorResponse> handleEventAlreadyRegisteredException(EventAlreadyRegisteredException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(TicketException.class)
    public ResponseEntity<ErrorResponse> handleTicketException(TicketException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }
}
