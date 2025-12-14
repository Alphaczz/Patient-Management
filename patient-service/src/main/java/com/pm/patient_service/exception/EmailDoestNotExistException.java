package com.pm.patient_service.exception;

public class EmailDoestNotExistException extends RuntimeException {
    public EmailDoestNotExistException(String message) {
        super(message);
    }
}
