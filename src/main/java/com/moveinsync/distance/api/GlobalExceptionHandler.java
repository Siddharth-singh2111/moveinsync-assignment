package com.moveinsync.distance.api;

import com.moveinsync.distance.api.dto.Dtos;
import com.moveinsync.distance.security.InvalidCredentialsException;
import com.moveinsync.distance.service.TripNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Turns exceptions into clean, non-leaking error responses. Client mistakes become 400/404 with
 * a short message; anything unexpected becomes a generic 500 and is logged server-side — never
 * a stack trace or any personal data (home location, phone, email) in the response.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(TripNotFoundException.class)
    public ResponseEntity<Dtos.ErrorResponse> notFound(TripNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new Dtos.ErrorResponse("trip_not_found", e.getMessage()));
    }

    @ExceptionHandler({MethodArgumentNotValidException.class, IllegalArgumentException.class})
    public ResponseEntity<Dtos.ErrorResponse> badRequest(Exception e) {
        return ResponseEntity.badRequest()
                .body(new Dtos.ErrorResponse("invalid_request", "the request was invalid"));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<Dtos.ErrorResponse> missingHeader(MissingRequestHeaderException e) {
        return ResponseEntity.badRequest()
                .body(new Dtos.ErrorResponse("missing_header", "missing required header: " + e.getHeaderName()));
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Dtos.ErrorResponse> invalidCredentials(InvalidCredentialsException e) {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(new Dtos.ErrorResponse("invalid_credentials", "invalid username or password"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Dtos.ErrorResponse> unexpected(Exception e) {
        log.error("unexpected error handling request", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new Dtos.ErrorResponse("internal_error", "an unexpected error occurred"));
    }
}
