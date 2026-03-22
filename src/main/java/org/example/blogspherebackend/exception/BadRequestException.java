package org.example.blogspherebackend.exception;

public class BadRequestException extends RuntimeException {

    public BadRequestException(String message) { super(message); }
}
