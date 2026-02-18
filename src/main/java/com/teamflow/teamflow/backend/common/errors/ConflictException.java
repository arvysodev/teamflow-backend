package com.teamflow.teamflow.backend.common.errors;

// use when trying to add/create something with already existing unique value

public class ConflictException extends RuntimeException{
    public ConflictException(String message) { super(message); }
}
