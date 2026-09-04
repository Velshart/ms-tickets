package me.mmtr.userservice.model.dto.messaging;

import java.io.Serializable;

public record UserDeletedEvent(Long id, String email) implements Serializable {
}
