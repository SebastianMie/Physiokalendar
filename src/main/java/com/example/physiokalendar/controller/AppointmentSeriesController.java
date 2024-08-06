// AppointmentSeriesController.java
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
    public ResponseEntity<AppointmentSeries> createOrUpdateAppointmentSeries(@RequestBody JSONAppointmentSeriesDTO appointmentSeriesDTO) {
        AppointmentSeries savedSeries = appointmentSeriesService.saveAppointmentSeries(appointmentSeriesDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(savedSeries);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteAppointmentSeries(@PathVariable Long id) {
        appointmentSeriesService.deleteAppointmentSeries(id);
        return ResponseEntity.noContent().build();
    }
}
