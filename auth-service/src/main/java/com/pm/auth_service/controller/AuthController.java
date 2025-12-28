package com.pm.auth_service.controller;

import com.pm.auth_service.dto.LoginRequestDto;
import com.pm.auth_service.dto.LoginResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
public class AuthController {
       @Operation(summary = "Generate token on user login")
      @PostMapping("/login")
    public ResponseEntity<LoginResponseDto> login(@RequestBody LoginRequestDto loginRequestDto) {
           Optional<String> tokenOptional =authService.authenticate(loginRequestDto);
            if(tokenOptional.isEmpty()){
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            return ResponseEntity.ok(new LoginResponseDto(tokenOptional.get()));
       }
}
