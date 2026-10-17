package com.pulsepass.dto.response;

public record AuthResponse(
        String accessToken,
        String tokenType,   // "Bearer"
        long expiresIn      // segundos (900)
) {

    @Override
    public String toString() {
        return "AuthResponse[accessToken=***, tokenType=" + tokenType + ", expiresIn=" + expiresIn + "]";
    }
}