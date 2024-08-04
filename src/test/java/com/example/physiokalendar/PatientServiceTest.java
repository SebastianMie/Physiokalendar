package com.example.physiokalendar;

import java.util.Date;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.physiokalendar.entity.Patient;
import com.example.physiokalendar.repository.PatientRepository;
import com.example.physiokalendar.service.PatientService;

@ExtendWith(MockitoExtension.class)
public class PatientServiceTest {

    @InjectMocks
    private PatientService patientService;

    @Mock
    private PatientRepository patientRepository;

    private Patient patient;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        patient = new Patient(1L, "John", "Doe", new Date(), new Date(), true);
    }

    @Test
    public void testCreatePatient() {
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        Patient created = patientService.createPatient(patient);

        assertEquals(patient.getFirstName(), created.getFirstName());
        verify(patientRepository, times(1)).save(patient);
    }

    @Test
    public void testDeletePatient() {
        doNothing().when(patientRepository).deleteById(anyLong());
        patientService.deletePatient(1L);
        verify(patientRepository, times(1)).deleteById(1L);
    }

    @Test
    public void testUpdatePatient() {
        when(patientRepository.findById(anyLong())).thenReturn(Optional.of(patient));
        when(patientRepository.save(any(Patient.class))).thenReturn(patient);

        Patient updatedPatient = new Patient(1L, "Jane", "Doe", new Date(), new Date(), false);
        Patient result = patientService.updatePatient(1L, updatedPatient);

        assertEquals("Jane", result.getFirstName());
        verify(patientRepository, times(1)).findById(1L);
        verify(patientRepository, times(1)).save(patient);
    }

    @Test
    public void testGetPatientById() {
        when(patientRepository.findById(anyLong())).thenReturn(Optional.of(patient));

        Patient foundPatient = patientService.getPatientById(1L);

        assertEquals(patient.getFirstName(), foundPatient.getFirstName());
        verify(patientRepository, times(1)).findById(1L);
    }

    @Test
    public void testGetPatientById_NotFound() {
        when(patientRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> patientService.getPatientById(1L));
    }
}
