package com.teamflow.teamflow.backend.common.errors;

public class ForbiddenException extends RuntimeException {
    public ForbiddenException(String message) { super(message); }
}
