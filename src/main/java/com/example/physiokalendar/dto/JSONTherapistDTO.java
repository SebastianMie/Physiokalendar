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
    private String name;
    private Date activeSince;
    private Date activeUntil;
    private List<Long> absenceIds;
    private List<JSONAbsenceDTO> absences;
    private List<Long> absenceExceptionIds;
    private List<JSONAbsenceExceptionDTO> absenceExceptions;
    // Getter and Setter

}
