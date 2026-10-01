package com.coworking.space.urlservice.infrastructure.excaptions;

public class UrlAlreadyExistsException extends RuntimeException {
    public UrlAlreadyExistsException(String message) {
        super(message);
    }
}
