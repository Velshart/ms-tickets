package me.mmtr.userservice.model.dto.response;

import me.mmtr.userservice.model.User;
import org.springframework.hateoas.server.core.Relation;

@Relation(collectionRelation = "users", itemRelation = "user")
public record UserResponse(Long id, String username, String email, String role) {

    public static UserResponse fromUser(User user) {
        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole().toString()
        );
    }
}
