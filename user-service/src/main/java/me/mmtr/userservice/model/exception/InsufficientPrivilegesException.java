package me.mmtr.userservice.model.exception;

import me.mmtr.common.web.ForbiddenException;

public class InsufficientPrivilegesException extends ForbiddenException {
    public InsufficientPrivilegesException() {
        super("Insufficient privileges to perform this operation");
    }
}
