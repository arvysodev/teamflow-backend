package com.teamflow.teamflow.backend.common.errors;

// use when getting request with invalid input format

public class BadRequestException extends RuntimeException{
    public BadRequestException(String message) { super(message); }
    public BadRequestException(String message, Throwable cause) { super(message, cause); }
}
