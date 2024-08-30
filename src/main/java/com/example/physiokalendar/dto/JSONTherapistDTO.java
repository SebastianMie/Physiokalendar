// JSONTherapistDTO.java
package com.example.physiokalendar.dto;

import java.util.Date;
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
    private Date activeSince;
    private Date activeUntil;
    private Boolean isActive;
    private List<Long> absenceIds;
    private List<JSONAbsenceDTO> absences;
    private List<Long> absenceExceptionIds;
    private List<JSONAbsenceExceptionDTO> absenceExceptions;
    // Getter and Setter

}
