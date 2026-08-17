package me.mmtr.userservice.model.exception;

public class InsufficientPrivilegesException extends RuntimeException {
    public InsufficientPrivilegesException() {
        super("Insufficient privileges to perform this operation");
    }
}
