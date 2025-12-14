package com.pm.patient_service.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GobalExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(GobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,String>> handleValidationException(MethodArgumentNotValidException ex){
        Map<String ,String> errors =new HashMap<>();
        ex.getBindingResult().getFieldErrors().forEach(error->errors.put(error.getField(),error.getDefaultMessage()));
         return ResponseEntity.badRequest().body(errors);
    }
    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String,String>> handleEmailAlreadyExistException(EmailAlreadyExistsException ex){
        Map<String ,String> errors =new HashMap<>();
          log.warn("Email address already exist");
         errors.put("message","Email Address already exist");
        return ResponseEntity.badRequest().body(errors);

    }

    @ExceptionHandler(PatientNotFoundException.class)
    public ResponseEntity<Map<String,String>> handleEPatientNotFoundException(PatientNotFoundException ex){
        Map<String ,String> errors =new HashMap<>();
        log.warn("Patient Not Found with given Id"+ex.getMessage());
        errors.put("message","Patient not found with given id");
        return ResponseEntity.badRequest().body(errors);

    }

    @ExceptionHandler(EmailDoestNotExistException.class)
    public ResponseEntity<Map<String,String>> handleEEmailDoestNotExistException(EmailDoestNotExistException ex){
        Map<String ,String> errors =new HashMap<>();
        log.warn("Email does not Found :: "+ex.getMessage());
        errors.put("message","Email Not found ");
        return ResponseEntity.badRequest().body(errors);

    }
}
