package me.mmtr.ticketservice.model.dto.event.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import me.mmtr.ticketservice.model.dto.event.CreateEventRequest;

public class EventDateValidator implements ConstraintValidator<ValidEventDates, CreateEventRequest> {
    @Override
    public boolean isValid(CreateEventRequest request, ConstraintValidatorContext context) {
        if (request == null ||
        request.startDate() == null ||
                request.endDate() == null
        ) {
            return true;
        }

        return request.startDate().isBefore(request.endDate());
    }
}
