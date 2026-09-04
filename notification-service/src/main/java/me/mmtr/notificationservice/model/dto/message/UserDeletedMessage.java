package me.mmtr.notificationservice.model.dto.message;

import java.io.Serializable;

public record UserDeletedMessage(
        Long id,
        String email
) implements Serializable {}
