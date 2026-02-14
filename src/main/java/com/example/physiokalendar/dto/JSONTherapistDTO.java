// JSONTherapistDTO.java
package com.example.physiokalendar.dto;

import java.time.LocalDateTime;
import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class JSONTherapistDTO {
    private Long id;
    private String userName;
    private String password;
    private String fullName;
    private String firstName;
    private String lastName;
    private String email;
    private String telefon;
    private LocalDateTime activeSince;
    private LocalDateTime activeUntil;
    private Boolean isActive;
    private List<Long> absenceIds;
    private List<JSONAbsenceDTO> absences;
    private List<Long> absenceExceptionIds;
    private List<JSONAbsenceExceptionDTO> absenceExceptions;
    // Getter and Setter

}
