package com.example.physiokalendar.controller;

import com.example.physiokalendar.entity.User;
import com.example.physiokalendar.service.UserService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.example.physiokalendar.dto.JSONLoginDTO;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @PostMapping("/login")
    public ResponseEntity<String> login(@RequestBody JSONLoginDTO loginDTO) {
        boolean isAuthenticated = userService.authenticateUser(loginDTO);
        if (isAuthenticated) {
            // Hier Token-Generierung und Rückgabe einfügen
            return ResponseEntity.ok("Login successful");
        } else {
            return ResponseEntity.status(401).body("Invalid credentials");
        }
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody JSONLoginDTO loginDTO) {
        User user = userService.registerUser(loginDTO);
        return ResponseEntity.ok(user);
    }
}
