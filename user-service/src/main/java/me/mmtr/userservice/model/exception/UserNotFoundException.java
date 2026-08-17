package me.mmtr.userservice.model.exception;

public class UserNotFoundException extends UserServiceException {
    public UserNotFoundException(Long id) {
        super(String.format("User with id %s not found", id));
    }
}
