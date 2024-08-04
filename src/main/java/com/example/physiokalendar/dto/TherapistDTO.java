package com.example.physiokalendar.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Data
@Setter
@EqualsAndHashCode
@Getter
@AllArgsConstructor
@NoArgsConstructor
public class TherapistDTO {
    private Long id;
    private String name;
    private Date activeSince;
    private Date activeUntil;


    // Getters and Setters
}