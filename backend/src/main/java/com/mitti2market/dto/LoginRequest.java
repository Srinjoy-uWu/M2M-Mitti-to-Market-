package com.mitti2market.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {
    private String email;
    private String phone;
    private String mobile;
    private String identifier;
    private String username;
    private String phoneOrEmail;
    private String emailOrPhone;

    @NotBlank(message = "Password is required")
    private String password;

    private String role;

    public String getIdentifier() {
        if (identifier != null && !identifier.trim().isEmpty()) return identifier.trim();
        if (email != null && !email.trim().isEmpty()) return email.trim();
        if (username != null && !username.trim().isEmpty()) return username.trim();
        if (phone != null && !phone.trim().isEmpty()) return phone.trim();
        if (mobile != null && !mobile.trim().isEmpty()) return mobile.trim();
        if (phoneOrEmail != null && !phoneOrEmail.trim().isEmpty()) return phoneOrEmail.trim();
        if (emailOrPhone != null && !emailOrPhone.trim().isEmpty()) return emailOrPhone.trim();
        return "";
    }
}
