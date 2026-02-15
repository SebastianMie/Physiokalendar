// PatientDTO.java
package com.example.physiokalendar.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

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
    private String fullName;
    private String firstName;
    private String lastName;
    private String email;
    private String telefon;
    private String street;
    private String houseNumber;
    private String postalCode;
    private String city;
    private LocalDate dateOfBirth;
    private String insuranceType;
    private String notes;
    private Boolean isActive;
    private LocalDateTime activeSince;
    private LocalDateTime activeUntil;
    private Boolean isBWO;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}