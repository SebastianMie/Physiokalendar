package com.example.physiokalendar.controller;

import com.example.physiokalendar.entity.User;
import com.example.physiokalendar.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.physiokalendar.config.JwtService;
import com.example.physiokalendar.dto.JSONLoginDTO;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtUtil;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody JSONLoginDTO loginDTO) {
        boolean isAuthenticated = userService.authenticateUser(loginDTO);
        if (isAuthenticated) {
            String token = jwtUtil.generateToken(loginDTO.getUsername());
            LoginResponse loginResponse = new LoginResponse(token);
            return ResponseEntity.status(HttpStatus.OK).body(loginResponse);
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody JSONLoginDTO loginDTO) {
        User user = userService.registerUser(loginDTO);
        return ResponseEntity.ok(user);
    }

    public class LoginResponse {

        private String token;
    
        public LoginResponse(String token) {
            this.token = token;
        }
    
        public String getToken() {
            return token;
        }
    
        public void setToken(String token) {
            this.token = token;
        }
    }
    
}
