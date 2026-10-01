package com.coworking.space.urlservice.infrastructure.excaptions;

public class ModerationUnavailableException extends RuntimeException{
    public ModerationUnavailableException(String message, Throwable cause) {
        super(message);
    }
}
