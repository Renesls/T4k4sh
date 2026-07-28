package com.t4kash.api.exception;

public class EmailDeliveryException extends RuntimeException {
    public EmailDeliveryException(String message, Throwable cause) {
        super(message, cause);
    }

    public EmailDeliveryException(String message) {
        super(message);
    }
}
