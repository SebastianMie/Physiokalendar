package com.example.physiokalendar.dto;

import java.util.Date;
import java.util.List;

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
public class JSONAppointmentSeriesDTO {
    private Long id;
    private JSONTherapistDTO therapist;
    private Long therapistId;
    private Long patientId;
    private String patientName;
    private Date startTime;
    private Date endTime;
    private String comment;
    private Date startDate;
    private Date endDate;
    private Integer weeklyFrequency;
    private String weekday;
    private List<JSONCancellationDTO> cancellations;
    private List<Long> cancellationIds;
    private Boolean isHotair;
    private Boolean isUltrasonic;
    private Boolean isElectric;
    // Getters and Setters
}
