package me.mmtr.userservice.model.dto.response;

import me.mmtr.userservice.model.User;

public record UserResponse(Long id, String username, String role) {

    public static UserResponse fromUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getRole().toString()
        );
    }
}
