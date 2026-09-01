package me.mmtr.userservice.model.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Username cannot be blank")
        @Size(
                min = 3,
                max = 50,
                message = "Username must be between 3 and 50 characters"
        )
        String username,

        @Email(message = "Email address must be valid")
        @NotBlank(message = "Email address cannot be bank")
        String email,

        @NotBlank(message = "Password cannot be blank")
        @Size(
                min = 6,
                message = "Password must be at least 6 characters long"
        )
        String password
) {}
