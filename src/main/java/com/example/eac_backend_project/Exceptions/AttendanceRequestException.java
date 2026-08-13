package com.example.eac_backend_project.Exceptions;

import org.springframework.http.HttpStatus;

public class AttendanceRequestException extends RuntimeException {
    private final HttpStatus status;

    public AttendanceRequestException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
