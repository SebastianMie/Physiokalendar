package com.example.physiokalendar.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JSONUserDTO {
    private Long id;
    private String username;
    private Long therapistId;
}
