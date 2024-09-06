package com.example.physiokalendar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.physiokalendar.config.JwtService;
import com.example.physiokalendar.dto.JSONLoginDTO;
import com.example.physiokalendar.dto.JSONUserDTO;
import com.example.physiokalendar.entity.User;
import com.example.physiokalendar.service.UserService;

@RestController
@RequestMapping("/api/auth")
public class UserController {

    @Autowired
    private UserService userService;

    @Autowired
    private JwtService jwtService;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody JSONLoginDTO loginDTO) {
        UserDetails userDetails = userService.authenticateUser(loginDTO.getUsername(), loginDTO.getPassword());
        if (userDetails != null) {
            String token = jwtService.generateToken(userDetails);
            LoginResponse loginResponse = new LoginResponse(token);
            return ResponseEntity.ok(loginResponse);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(new LoginResponse("Invalid credentials"));
        }
    }

    @PostMapping("/register")
    public ResponseEntity<User> register(@RequestBody JSONLoginDTO registerDTO) {
        User user = userService.registerUser(registerDTO.getUsername(), registerDTO.getPassword());
        return ResponseEntity.status(HttpStatus.CREATED).body(user);
    }

    @GetMapping("/user")
    public ResponseEntity<JSONUserDTO> getUserDetails(@RequestHeader("Authorization") String authHeader) {
        String token = authHeader.replace("Bearer ", "");
        String username = jwtService.extractUsername(token);
        UserDetails user = userService.getUserDetails(username);
        JSONUserDTO userDTO = new JSONUserDTO();
        userDTO.setUsername(user.getUsername());
        return ResponseEntity.ok(userDTO);
    }

    public static class LoginResponse {
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

