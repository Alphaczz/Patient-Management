package com.pm.auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class LoginRequestDto {


    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid email address")
    private String email;
    @NotBlank(message = "Password is required")
    @Size(min = 8,message = "Password should must be atleast 8 character")
    private String password;
    public LoginRequestDto() {
    }
    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

}
