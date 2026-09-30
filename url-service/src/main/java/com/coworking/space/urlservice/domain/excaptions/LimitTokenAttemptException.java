package com.coworking.space.urlservice.domain.excaptions;

public class LimitTokenAttemptException extends RuntimeException{
    public LimitTokenAttemptException(String message) {
        super(message);
    }
}
