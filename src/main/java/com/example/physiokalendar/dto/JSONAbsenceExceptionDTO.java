package com.example.physiokalendar.dto;

import java.util.Date;

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
public class JSONAbsenceExceptionDTO {
    private Long id;
    private Date date;
    private String weekday;
    private Date startTime;
    private Date endTime;

    // Getters and Setters
}