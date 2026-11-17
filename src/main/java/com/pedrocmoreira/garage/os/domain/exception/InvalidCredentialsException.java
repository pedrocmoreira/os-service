package com.pedrocmoreira.garage.os.domain.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Usuário ou senha incorretos");
    }
}
