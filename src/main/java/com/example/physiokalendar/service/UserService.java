package com.example.physiokalendar.service;

import com.example.physiokalendar.dto.JSONLoginDTO;
import com.example.physiokalendar.entity.User;
import com.example.physiokalendar.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public boolean authenticateUser(JSONLoginDTO loginDTO) {
        return userRepository.findByUsername(loginDTO.getUsername())
                .map(user -> passwordEncoder.matches(loginDTO.getPassword(), user.getPassword()))
                .orElse(false);
    }

    public User registerUser(JSONLoginDTO loginDTO) {
        User user = new User();
        user.setUsername(loginDTO.getUsername());
        user.setPassword(passwordEncoder.encode(loginDTO.getPassword())); // Passwort-Hashing
        // Hier kannst du auch `therapistId` setzen, falls erforderlich
        return userRepository.save(user);
    }
}
