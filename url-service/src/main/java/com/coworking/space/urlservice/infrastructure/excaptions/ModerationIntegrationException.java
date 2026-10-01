package com.coworking.space.urlservice.infrastructure.excaptions;

public class ModerationIntegrationException extends RuntimeException{
    public ModerationIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
