package com.teamflow.teamflow.backend.common.errors;

// use when getting request with non-existing object

public class NotFoundException extends RuntimeException{
    public NotFoundException(String message) { super(message); }
    public NotFoundException(String message, Throwable cause) { super(message, cause); }
}
