package me.mmtr.notificationservice.exception;

public class UserEmailNotFoundException extends RuntimeException {
    public UserEmailNotFoundException(Long id) {
        super("No email found for user with id" + id);
    }
}
