package com.pm.auth_service.service;

import com.pm.auth_service.dto.LoginRequestDto;

import com.pm.auth_service.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {
    private final UserService userService;
    public final PasswordEncoder passwordEncoder;
    public final JwtUtil jwtUtil;
    public AuthService(UserService userService,PasswordEncoder passwordEncoder,JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }
  public Optional<String> authenticate(LoginRequestDto loginRequestDto) {
      Optional<String> token =userService.findByEmail(loginRequestDto.getEmail()).
              filter(u -> passwordEncoder.matches(loginRequestDto.getPassword(),u.getPassword()))
              .map(u->jwtUtil.generateToken(u.getEmail(),u.getRole()));

      return token;


  }

    public boolean validateToken(String token) {
        try{
         jwtUtil.validateToken(token);
         return true;
        }catch (JwtException e){
            return false;
        }
    }
}
