package com.example.eac_backend_project.Exceptions;

public class PayrollValidationException extends RuntimeException {
    public PayrollValidationException(String message) {
        super(message);
    }
}