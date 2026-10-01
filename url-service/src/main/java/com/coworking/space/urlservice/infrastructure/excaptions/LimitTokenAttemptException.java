package com.coworking.space.urlservice.infrastructure.excaptions;

public class LimitTokenAttemptException extends RuntimeException{
    public LimitTokenAttemptException(String message) {
        super(message);
    }
}
