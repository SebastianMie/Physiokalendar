// PatientDTO.java
package com.example.physiokalendar.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@ToString
@Data
@Setter
@EqualsAndHashCode
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class JSONPatientDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private long activeSince;
    private long activeUntil;
    private Boolean isBWO;

    // Getters and Setters
}