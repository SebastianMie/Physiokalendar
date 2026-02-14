package com.example.physiokalendar.controller;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import com.example.physiokalendar.entity.Role;
import com.example.physiokalendar.entity.User;
import com.example.physiokalendar.repository.UserRepository;

@RestController
@RequestMapping("/api/admin/users")
@PreAuthorize("hasRole('ADMIN') or hasRole('RECEPTION')")
public class AdminController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminController(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<Map<String, Object>> getAllUsers() {
        return userRepository.findAll().stream()
            .map(user -> Map.<String, Object>of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole() != null ? user.getRole().name() : "THERAPIST",
                "therapistId", user.getTherapistId() != null ? user.getTherapistId() : 0
            ))
            .collect(Collectors.toList());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getUserById(@PathVariable Long id) {
        return userRepository.findById(id)
            .map(user -> ResponseEntity.ok(Map.<String, Object>of(
                "id", user.getId(),
                "username", user.getUsername(),
                "role", user.getRole() != null ? user.getRole().name() : "THERAPIST",
                "therapistId", user.getTherapistId() != null ? user.getTherapistId() : 0
            )))
            .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> createUser(@RequestBody Map<String, Object> request) {
        String username = (String) request.get("username");
        String password = (String) request.get("password");
        String roleStr = (String) request.get("role");
        Object therapistIdObj = request.get("therapistId");

        if (username == null || password == null) {
            return ResponseEntity.badRequest().build();
        }

        if (userRepository.findByUsername(username).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Username already exists"));
        }

        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(roleStr != null ? Role.valueOf(roleStr) : Role.THERAPIST);

        if (therapistIdObj != null) {
            if (therapistIdObj instanceof Integer) {
                user.setTherapistId(((Integer) therapistIdObj).longValue());
            } else if (therapistIdObj instanceof Long) {
                user.setTherapistId((Long) therapistIdObj);
            }
        }

        User saved = userRepository.save(user);

        return ResponseEntity.ok(Map.<String, Object>of(
            "id", saved.getId(),
            "username", saved.getUsername(),
            "role", saved.getRole().name(),
            "therapistId", saved.getTherapistId() != null ? saved.getTherapistId() : 0
        ));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateUser(@PathVariable Long id, @RequestBody Map<String, Object> request) {
        return userRepository.findById(id)
            .map(user -> {
                String username = (String) request.get("username");
                String password = (String) request.get("password");
                String roleStr = (String) request.get("role");
                Object therapistIdObj = request.get("therapistId");

                if (username != null && !username.isBlank()) {
                    user.setUsername(username);
                }
                if (password != null && !password.isBlank()) {
                    user.setPassword(passwordEncoder.encode(password));
                }
                if (roleStr != null) {
                    user.setRole(Role.valueOf(roleStr));
                }
                if (therapistIdObj != null) {
                    if (therapistIdObj instanceof Integer) {
                        user.setTherapistId(((Integer) therapistIdObj).longValue());
                    } else if (therapistIdObj instanceof Long) {
                        user.setTherapistId((Long) therapistIdObj);
                    }
                }

                User saved = userRepository.save(user);
                return ResponseEntity.ok(Map.<String, Object>of(
                    "id", saved.getId(),
                    "username", saved.getUsername(),
                    "role", saved.getRole().name(),
                    "therapistId", saved.getTherapistId() != null ? saved.getTherapistId() : 0
                ));
            })
            .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        if (userRepository.existsById(id)) {
            userRepository.deleteById(id);
            return ResponseEntity.ok().build();
        }
        return ResponseEntity.notFound().build();
    }
}
