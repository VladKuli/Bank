package com.bank.bank_system.web.account;

import com.bank.bank_system.exception.AccountBalanceMustBeZeroException;
import com.bank.bank_system.exception.AccountNotFoundException;
import com.bank.bank_system.exception.AccountNotOpenException;
import com.bank.bank_system.exception.BlockedClientOperationException;
import com.bank.bank_system.exception.CurrencyMismatchException;
import com.bank.bank_system.exception.InsufficientFundsException;
import com.bank.bank_system.exception.InvalidAmountException;
import com.bank.bank_system.exception.SameAccountTransferException;
import com.bank.bank_system.exception.ClientNotFoundException;
import com.bank.bank_system.web.ErrorResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class AccountExceptionHandler {

    @ExceptionHandler({AccountNotFoundException.class, ClientNotFoundException.class})
    public ResponseEntity<ErrorResponse> notFound(RuntimeException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(BlockedClientOperationException.class)
    public ResponseEntity<ErrorResponse> forbidden(BlockedClientOperationException exception) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler(InsufficientFundsException.class)
    public ResponseEntity<ErrorResponse> conflict(InsufficientFundsException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse(exception.getMessage()));
    }

    @ExceptionHandler({
            AccountBalanceMustBeZeroException.class,
            AccountNotOpenException.class,
            CurrencyMismatchException.class,
            InvalidAmountException.class,
            SameAccountTransferException.class
    })
    public ResponseEntity<ErrorResponse> badRequest(RuntimeException exception) {
        return ResponseEntity.badRequest().body(new ErrorResponse(exception.getMessage()));
    }

}
