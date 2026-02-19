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
public class JSONAbsenceDTO {
    private Long id;
    private Long therapistId;
    private String date;           // Changed from Date to String (YYYY-MM-DD format)
    private String endDate;        // Changed from Date to String (YYYY-MM-DD format)
    private String weekday;
    private String startTime;      // Changed from Date to String (HH:mm:ss or ISO format)
    private String endTime;        // Changed from Date to String (HH:mm:ss or ISO format)
    private String reason;
    private String absenceType;
}
