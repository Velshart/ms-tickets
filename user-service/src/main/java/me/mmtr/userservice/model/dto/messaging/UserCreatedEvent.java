package me.mmtr.userservice.model.dto.messaging;

import java.io.Serializable;

public record UserCreatedEvent(Long id, String email) implements Serializable {}
