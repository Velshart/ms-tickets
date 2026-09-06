package me.mmtr.ticketservice.model.exception;

public class TicketSalesClosedException extends TicketException {
    public TicketSalesClosedException(Long id) {
        super(
                "Ticket sales closed for event with id " + id
        );
    }
}
