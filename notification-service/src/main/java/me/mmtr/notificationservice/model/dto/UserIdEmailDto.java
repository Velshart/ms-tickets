package me.mmtr.notificationservice.model.dto;

import me.mmtr.notificationservice.model.UserIdEmail;

import java.io.Serializable;

public record UserIdEmailDto(Long userId, String email) implements Serializable {
    public static UserIdEmailDto from(UserIdEmail userIdEmail) {
        return new UserIdEmailDto(
                userIdEmail.getUserId(),
                userIdEmail.getEmail()
        );
    }
}
