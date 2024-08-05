package com.example.physiokalendar.service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.dto.JSONTherapistDTO;
import com.example.physiokalendar.entity.Therapist;
import com.example.physiokalendar.repository.TherapistRepository;

@Service
public class TherapistService {

    @Autowired
    private TherapistRepository therapistRepository;

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
        Therapist therapist = convertDTOToEntity(dto);
        return therapistRepository.save(therapist);
    }

    public Therapist updateTherapist(Long id, JSONTherapistDTO dto) {
        Therapist existingTherapist = therapistRepository.findById(id).orElseThrow(() -> new RuntimeException("Therapist not found"));
        existingTherapist.setName(dto.getName());
        existingTherapist.setActiveSince(new Date(dto.getActiveSince()));
        existingTherapist.setActiveUntil(new Date(dto.getActiveUntil()));
        // Weitere Feldaktualisierungen, falls vorhanden
        return therapistRepository.save(existingTherapist);
    }

    public void deleteTherapist(Long id) {
        therapistRepository.deleteById(id);
    }

    private JSONTherapistDTO convertEntityToDTO(Therapist therapist) {
        JSONTherapistDTO dto = new JSONTherapistDTO();
        dto.setId(therapist.getId().toString());
        dto.setName(therapist.getName());
        dto.setActiveSince(therapist.getActiveSince().getTime());
        dto.setActiveUntil(therapist.getActiveUntil().getTime());
        // Weitere Felder falls nötig
        return dto;
    }

    private Therapist convertDTOToEntity(JSONTherapistDTO dto) {
        Therapist therapist = new Therapist();
        therapist.setId(Long.valueOf(dto.getId()));
        therapist.setName(dto.getName());
        therapist.setActiveSince(new Date(dto.getActiveSince()));
        therapist.setActiveUntil(new Date(dto.getActiveUntil()));
        // Weitere Felder falls nötig
        return therapist;
    }

}
