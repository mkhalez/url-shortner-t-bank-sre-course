package com.coworking.space.urlservice.infrastructure.excaptions;

public class NotValidCustomCodeException extends RuntimeException {
    public NotValidCustomCodeException(String message) {
        super(message);
    }
}
