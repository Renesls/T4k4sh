package com.t4kash.api.exception;

public class InvalidWebhookSignatureException extends RuntimeException {
    public InvalidWebhookSignatureException(String message) {
        super(message);
    }
}
