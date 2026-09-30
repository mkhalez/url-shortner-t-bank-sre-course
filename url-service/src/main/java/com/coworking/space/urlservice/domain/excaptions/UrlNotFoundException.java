package com.coworking.space.urlservice.domain.excaptions;

public class UrlNotFoundException extends RuntimeException{
    public UrlNotFoundException(String message) {
        super(message);
    }
}
