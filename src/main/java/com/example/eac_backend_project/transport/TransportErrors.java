package com.example.eac_backend_project.transport;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.dao.*;
import java.util.Map;

@RestControllerAdvice(assignableTypes=TransportController.class)
public class TransportErrors {
 @ExceptionHandler(ResponseStatusException.class) public ResponseEntity<?> status(ResponseStatusException error){return ResponseEntity.status(error.getStatusCode()).body(Map.of("message",error.getReason()==null?"Transport request failed":error.getReason()));}
 @ExceptionHandler({OptimisticLockingFailureException.class,PessimisticLockingFailureException.class}) public ResponseEntity<?> concurrent(RuntimeException error){return ResponseEntity.status(409).body(Map.of("message","This record changed or is being updated. Refresh the record before trying again."));}
 @ExceptionHandler(DataIntegrityViolationException.class) public ResponseEntity<?> conflict(DataIntegrityViolationException error){return ResponseEntity.status(409).body(Map.of("message","This change conflicts with an existing record. Refresh and check the selected references."));}
 @ExceptionHandler(java.io.IOException.class) public ResponseEntity<?> file(java.io.IOException error){return ResponseEntity.status(500).body(Map.of("message","The file operation could not be completed. Retry or contact an administrator."));}
}
