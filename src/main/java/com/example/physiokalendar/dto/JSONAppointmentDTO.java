// AppointmentDTO.java
package com.example.physiokalendar.dto;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class JSONAppointmentDTO {
    private Long id;
    private JSONTherapistDTO therapist;
    private Long therapistId;
    private Date date;
    private Long patientId;
    private Date startTime;
    private Date endTime;
    private String comment;
    private Boolean isHotair;
    private Boolean isUltrasonic;
    private Boolean isElectric;
}
