package com.moveinsync.distance.security;

/** Login failed: unknown user or wrong password. Maps to 401. */
public class InvalidCredentialsException extends RuntimeException {

    public InvalidCredentialsException() {
        super("invalid username or password");
    }
}
