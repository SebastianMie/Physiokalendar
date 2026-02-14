package com.example.physiokalendar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import com.example.physiokalendar.entity.User;
import com.example.physiokalendar.repository.UserRepository;
import com.example.physiokalendar.config.JwtService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin(origins = "http://localhost:4200")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtService jwtService;

    /**
     * Login endpoint
     * POST /api/auth/login
     * Body: { "username": "...", "password": "..." }
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest request) {
        Optional<User> userOpt = userRepository.findByUsername(request.getUsername());

        if (userOpt.isEmpty()) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "Invalid credentials"
            ));
        }

        User user = userOpt.get();

        // Verify password
        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "Invalid credentials"
            ));
        }

        // Generate JWT token
        String token = jwtService.generateToken(user.getUsername());

        Map<String, Object> response = new HashMap<>();
        response.put("token", token);
        response.put("expiresIn", 86400); // 24 hours
        response.put("user", Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "role", "THERAPIST"
        ));

        return ResponseEntity.ok(response);
    }

    /**
     * Get current user endpoint
     * GET /api/auth/me
     * Header: Authorization: Bearer <token>
     */
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(@RequestHeader(value = "Authorization", required = false) String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "No token provided"
            ));
        }

        String token = authHeader.substring(7);
        String username = jwtService.extractUsername(token);

        if (username == null || !jwtService.isTokenValid(token, username)) {
            return ResponseEntity.status(401).body(Map.of(
                "error", "Invalid token"
            ));
        }

        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "User not found"
            ));
        }

        User user = userOpt.get();
        return ResponseEntity.ok(Map.of(
            "id", user.getId(),
            "username", user.getUsername(),
            "role", "THERAPIST"
        ));
    }

    /**
     * DEBUG: Generate password hash (remove in production!)
     */
    @GetMapping("/hash/{password}")
    public ResponseEntity<?> generateHash(@PathVariable String password) {
        String hash = passwordEncoder.encode(password);
        return ResponseEntity.ok(Map.of("hash", hash));
    }

    /**
     * Login request DTO
     */
    public static class LoginRequest {
        public String username;
        public String password;

        public LoginRequest() {}

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }
}
