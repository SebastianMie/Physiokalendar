package com.example.physiokalendar.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.physiokalendar.dto.JSONAppointmentSeriesDTO;
import com.example.physiokalendar.dto.JSONCancellationDTO;
import com.example.physiokalendar.entity.AppointmentSeries;
import com.example.physiokalendar.service.AppointmentSeriesService;

@RestController
@RequestMapping("/api/appointmentseries")
public class AppointmentSeriesController {

    @Autowired
    private AppointmentSeriesService appointmentSeriesService;

    @GetMapping
    public ResponseEntity<List<AppointmentSeries>> getAllAppointmentSeries() {
        List<AppointmentSeries> series = appointmentSeriesService.getAllAppointmentSeries();
        return ResponseEntity.ok(series);
    }

    @GetMapping("/{id}")
    public ResponseEntity<AppointmentSeries> getAppointmentSeriesById(@PathVariable Long id) {
        return appointmentSeriesService.getAppointmentSeriesById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<String> createOrUpdateAppointmentSeries(@RequestBody JSONAppointmentSeriesDTO appointmentSeriesDTO) {
        try {
            AppointmentSeries savedSeries = appointmentSeriesService.saveAppointmentSeries(appointmentSeriesDTO);
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body("Appointment Series successfully created with ID: " + savedSeries.getId());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("Invalid data provided: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Conflict with existing appointments: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error creating appointment series.");
        }
    }

    @PostMapping("/{id}/cancellations")
    public ResponseEntity<?> addCancellations(@PathVariable Long id, @RequestBody List<JSONCancellationDTO> cancellationDTOs) {
        try {
            AppointmentSeries updatedSeries = appointmentSeriesService.addCancellations(id, cancellationDTOs);
            return ResponseEntity.ok(updatedSeries);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("Appointment Series not found: " + e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error adding cancellations.");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointmentSeries(@PathVariable Long id) {
        try {
            appointmentSeriesService.deleteAppointmentSeries(id);
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .build();
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .build();
        }
    }
}
