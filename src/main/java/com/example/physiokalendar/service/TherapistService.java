package com.example.physiokalendar.service;

import java.util.List;
import java.util.stream.Collectors;

import com.example.physiokalendar.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.dto.JSONTherapistDTO;
import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.repository.TherapistRepository;

@Service
public class TherapistService {

    @Autowired
    private TherapistRepository therapistRepository;

    @Autowired
    private UserService userService;

    @Autowired
    private PasswordEncoder passwordEncoder;

    public List<JSONTherapistDTO> getAllTherapists() {
        return therapistRepository.findAll().stream()
                .map(this::convertEntityToDTO)
                .collect(Collectors.toList());
    }

    public JSONTherapistDTO getTherapistById(Long id) {
        Therapist therapist = therapistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Therapist not found"));
        return convertEntityToDTO(therapist);
    }

    public Therapist createTherapist(JSONTherapistDTO dto) {
        // Konvertiere DTO zu einer Therapeuten-Entität
        Therapist therapist = convertDTOToEntity(dto);

        // Speichere den Therapeuten in der Datenbank, um die ID zu generieren
        Therapist savedTherapist = therapistRepository.save(therapist);

        // Benutzername und Passwort generieren
        String firstName = savedTherapist.getFirstName();
        String lastName = savedTherapist.getLastName();

        // Stelle sicher, dass der Nachname mindestens 3 Zeichen hat
        String username = firstName + (lastName.length() >= 3 ? lastName.substring(0, 3) : lastName);
        String password = firstName + (lastName.length() >= 3 ? lastName.substring(0, 3) : lastName);

        // Erstelle einen neuen Benutzer und setze die Standardwerte
        User user = new User();
        user.setTherapistId(savedTherapist.getId());  // Hier die generierte Therapeuten-ID setzen
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));  // Passwort verschlüsseln

        // Speichere den Benutzer
        userService.registerUser(user);

        // Gebe den gespeicherten Therapeuten zurück
        return savedTherapist;
    }

    public Therapist updateTherapist(Long id, JSONTherapistDTO dto) {
        Therapist existingTherapist = therapistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Therapist not found"));
        existingTherapist.setFirstName(dto.getFirstName());
        existingTherapist.setLastName(dto.getLastName());
        existingTherapist.setFullName(dto.getFirstName() + " " + dto.getLastName());
        existingTherapist.setEmail(dto.getEmail());
        existingTherapist.setTelefon(dto.getTelefon());
        existingTherapist.setActiveSince(dto.getActiveSince());
        existingTherapist.setActiveUntil(dto.getActiveUntil());
        existingTherapist.setIsActive(dto.getIsActive());
        return therapistRepository.save(existingTherapist);
    }

    public void deleteTherapist(Long id) {
        therapistRepository.deleteById(id);
    }

    public JSONTherapistDTO convertEntityToDTO(Therapist therapist) {
        JSONTherapistDTO dto = new JSONTherapistDTO();
        dto.setId(therapist.getId());
        dto.setFirstName(therapist.getFirstName());
        dto.setLastName(therapist.getLastName());
        dto.setFullName(therapist.getFullName());
        dto.setEmail(therapist.getEmail());
        dto.setTelefon(therapist.getTelefon());
        dto.setActiveSince(therapist.getActiveSince());
        dto.setActiveUntil(therapist.getActiveUntil());
        dto.setIsActive(therapist.getIsActive());
        return dto;
    }

    public Therapist convertDTOToEntity(JSONTherapistDTO dto) {
        Therapist therapist = new Therapist();
        therapist.setId(dto.getId());
        therapist.setFirstName(dto.getFirstName());
        therapist.setLastName(dto.getLastName());
        therapist.setFullName(dto.getFullName());
        therapist.setActiveSince(dto.getActiveSince());
        therapist.setActiveUntil(dto.getActiveUntil());
        therapist.setIsActive(dto.getIsActive());
        return therapist;
    }

}
