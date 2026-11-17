package com.pedrocmoreira.garage.os.presentation.dto;

import jakarta.validation.constraints.NotBlank;

public final class AuthDTO {
    private AuthDTO() {}
    public record LoginRequest(@NotBlank String username, @NotBlank String password) {}
    public record TokenResponse(String token, String type, long expiresInMs) {}
}
