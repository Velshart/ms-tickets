package me.mmtr.userservice.model.exception;

import me.mmtr.common.web.NotFoundException;

public class UserNotFoundException extends NotFoundException {
    public UserNotFoundException(Long id) {
        super(String.format("User with id %s not found", id));
    }
}
