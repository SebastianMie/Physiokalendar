package com.example.physiokalendar.controller;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.physiokalendar.entity.AppointmentSeries;
import com.example.physiokalendar.service.AppointmentSeriesService;

@RestController
@RequestMapping("/api/appointmentseries")
public class AppointmentSeriesController {

    @Autowired
    private AppointmentSeriesService appointmentSeriesService;

    @GetMapping
    public List<AppointmentSeries> getAllAppointmentSeries() {
        return appointmentSeriesService.getAllAppointmentSeries();
    }

    @GetMapping("/{id}")
    public Optional<AppointmentSeries> getAppointmentSeriesById(@PathVariable Long id) {
        return appointmentSeriesService.getAppointmentSeriesById(id);
    }

    @PostMapping
    public AppointmentSeries createOrUpdateAppointmentSeries(@RequestBody AppointmentSeries appointmentSeries) {
        return appointmentSeriesService.saveAppointmentSeries(appointmentSeries);
    }

    @DeleteMapping("/{id}")
    public void deleteAppointmentSeries(@PathVariable Long id) {
        appointmentSeriesService.deleteAppointmentSeries(id);
    }
}
