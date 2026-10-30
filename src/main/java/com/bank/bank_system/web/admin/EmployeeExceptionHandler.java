package com.bank.bank_system.web.admin;

import com.bank.bank_system.exception.DuplicateEmployeeUsernameException;
import com.bank.bank_system.exception.EmployeeNotFoundException;
import com.bank.bank_system.exception.LastEnabledAdministratorException;
import com.bank.bank_system.exception.SelfDisableException;
import com.bank.bank_system.web.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class EmployeeExceptionHandler {

    @ExceptionHandler(DuplicateEmployeeUsernameException.class)
    public ResponseEntity<ErrorResponse> duplicateUsername(DuplicateEmployeeUsernameException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ErrorResponse> notFound(EmployeeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler({LastEnabledAdministratorException.class, SelfDisableException.class})
    public ResponseEntity<ErrorResponse> conflict(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
    }
}
