package com.example.physiokalendar.service;

import java.util.List;
import java.util.stream.Collectors;

import com.example.physiokalendar.entity.AuditAction;
import com.example.physiokalendar.entity.AuditEntityType;
import com.example.physiokalendar.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Autowired
    private AuditService auditService;

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

    @Transactional
    public Therapist createTherapist(JSONTherapistDTO dto) {
        // Konvertiere DTO zu einer Therapeuten-Entität
        Therapist therapist = convertDTOToEntity(dto);

        // Speichere den Therapeuten in der Datenbank, um die ID zu generieren
        Therapist savedTherapist = therapistRepository.save(therapist);

        // Audit-Log
        auditService.record(AuditService.builder()
                .actor(getCurrentUserId(), getCurrentUsername())
                .entity(AuditEntityType.THERAPIST, savedTherapist.getId())
                .action(AuditAction.CREATE)
                .after(auditService.toAuditJson(savedTherapist)));

        // Benutzername und Passwort generieren
        String firstName = savedTherapist.getFirstName();
        String lastName = savedTherapist.getLastName();

        // Stelle sicher, dass der Nachname mindestens 3 Zeichen hat
        String username = firstName + (lastName.length() >= 3 ? lastName.substring(0, 3) : lastName);
        String password = firstName + (lastName.length() >= 3 ? lastName.substring(0, 3) : lastName);

        // Erstelle einen neuen Benutzer und setze die Standardwerte
        User user = new User();
        user.setTherapistId(savedTherapist.getId());
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(password));

        // Speichere den Benutzer
        userService.registerUser(user);

        return savedTherapist;
    }

    @Transactional
    public Therapist updateTherapist(Long id, JSONTherapistDTO dto) {
        Therapist existingTherapist = therapistRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Therapist not found"));

        String beforeJson = auditService.toAuditJson(existingTherapist);

        existingTherapist.setFirstName(dto.getFirstName());
        existingTherapist.setLastName(dto.getLastName());
        existingTherapist.setFullName(dto.getFirstName() + " " + dto.getLastName());
        existingTherapist.setEmail(dto.getEmail());
        existingTherapist.setTelefon(dto.getTelefon());
        existingTherapist.setActiveSince(dto.getActiveSince());
        existingTherapist.setActiveUntil(dto.getActiveUntil());
        existingTherapist.setIsActive(dto.getIsActive());

        Therapist updated = therapistRepository.save(existingTherapist);

        // Audit-Log
        auditService.record(AuditService.builder()
                .actor(getCurrentUserId(), getCurrentUsername())
                .entity(AuditEntityType.THERAPIST, id)
                .action(AuditAction.UPDATE)
                .before(beforeJson)
                .after(auditService.toAuditJson(updated)));

        return updated;
    }

    @Transactional
    public void deleteTherapist(Long id) {
        Therapist existing = therapistRepository.findById(id).orElse(null);
        if (existing != null) {
            String beforeJson = auditService.toAuditJson(existing);
            therapistRepository.deleteById(id);

            // Audit-Log
            auditService.record(AuditService.builder()
                    .actor(getCurrentUserId(), getCurrentUsername())
                    .entity(AuditEntityType.THERAPIST, id)
                    .action(AuditAction.DELETE)
                    .before(beforeJson));
        }
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return ((User) auth.getPrincipal()).getId();
        }
        return null;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        return "system";
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
