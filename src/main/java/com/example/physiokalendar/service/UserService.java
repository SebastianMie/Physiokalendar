package com.example.physiokalendar.service;

import com.example.physiokalendar.dto.JSONUserDTO;
import com.example.physiokalendar.entity.User;
import com.example.physiokalendar.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        UserDetails user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return user;
    }

    public UserDetails authenticateUser(String username, String password) {
        UserDetails userDetails = loadUserByUsername(username);
        if (passwordEncoder.matches(password, userDetails.getPassword())) {
            return userDetails;
        }
        return null;
    }

    public User registerUser(String username, String password) {
        User user = new User();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));
        return userRepository.save(user);
    }

    public UserDetails getUserDetails(String username) {
        return userRepository.findByUsername(username)
                .orElse(null);
    }

    public JSONUserDTO convertToUserDTO(User user) {
        JSONUserDTO userDTO = new JSONUserDTO();
        userDTO.setId(user.getId());
        userDTO.setUsername(user.getUsername());
        userDTO.setTherapistId(user.getTherapistId()); // Falls `therapistId` ein Feld in deiner `User`-Klasse ist
        return userDTO;
    }

    public User convertToUser(JSONUserDTO userDTO) {
        User user = new User();
        user.setId(userDTO.getId());
        user.setUsername(userDTO.getUsername());
        user.setTherapistId(userDTO.getTherapistId()); // Falls `therapistId` ein Feld in deiner `User`-Klasse ist
        return user;
    }
    
    
}
