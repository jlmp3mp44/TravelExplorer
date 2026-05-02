package com.travel.explorer.security.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginRequest {
    /**
     * Username or email address (both are accepted for authentication).
     */
    @NotBlank
    private String username;
    @NotBlank
    private String password;
}
