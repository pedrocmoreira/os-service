package com.pedrocmoreira.garage.os.presentation.controller;

import com.pedrocmoreira.garage.os.domain.exception.InvalidCredentialsException;
import com.pedrocmoreira.garage.os.infrastructure.security.JwtTokenProvider;
import com.pedrocmoreira.garage.os.presentation.dto.AuthDTO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticação")
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @PostMapping("/login")
    @Operation(summary = "Login e geração do token JWT (válido nos 3 microsserviços)")
    public AuthDTO.TokenResponse login(@Valid @RequestBody AuthDTO.LoginRequest request) {
        try {
            var auth = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password()));
            String token = tokenProvider.generateToken((UserDetails) auth.getPrincipal());
            return new AuthDTO.TokenResponse(token, "Bearer", tokenProvider.getExpirationMs());
        } catch (AuthenticationException e) {
            throw new InvalidCredentialsException();
        }
    }
}
