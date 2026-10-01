package com.coworking.space.urlservice.infrastructure.excaptions;

public class UrlNotFoundException extends RuntimeException{
    public UrlNotFoundException(String message) {
        super(message);
    }
}
