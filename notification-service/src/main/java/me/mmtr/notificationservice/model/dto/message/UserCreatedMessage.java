package me.mmtr.notificationservice.model.dto.message;

import java.io.Serializable;

public record UserCreatedMessage(
        Long id,
        String email
) implements Serializable {}
