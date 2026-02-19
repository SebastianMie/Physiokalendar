package com.example.physiokalendar.service;

import com.example.physiokalendar.dto.AppointmentDraftDTO;
import com.example.physiokalendar.dto.AppointmentSaveResult;
import com.example.physiokalendar.dto.ConflictCheckDTO;
import com.example.physiokalendar.dto.JSONAppointmentDTO;
import com.example.physiokalendar.entity.*;
import com.example.physiokalendar.repository.AppointmentRepository;
import com.example.physiokalendar.repository.CancellationRepository;
import com.example.physiokalendar.repository.TherapistRepository;
import com.example.physiokalendar.repository.PatientRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private TherapistRepository therapistRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private PatientService patientService;

    @Autowired
    private TherapistService therapistService;

    @Autowired
    private AbsenceService absenceService;

    @Autowired
    private CancellationRepository cancellationRepository;

    @Autowired
    private AuditService auditService;

    @Autowired
    private ConflictService conflictService;

    public List<Appointment> getAllAppointments() {
        return appointmentRepository.findAll();
    }

    /**
     * Get paginated single appointments (non-series) with optional filters.
     * Used for lazy loading in the appointment overview.
     */
    public Page<Appointment> getSingleAppointmentsPaginated(
            LocalDate dateFrom,
            LocalDate dateTo,
            Long therapistId,
            AppointmentStatus status,
            String searchTerm,
            Pageable pageable) {
        return appointmentRepository.findSingleAppointmentsFiltered(
                dateFrom, dateTo, therapistId, status, searchTerm, pageable);
    }

    /**
     * Get paginated appointments with optional appointment type filter.
     * Used for therapist detail view with faceted search.
     * @param appointmentType null=all, true=series only, false=single only
     */
    public Page<Appointment> getAppointmentsPaginated(
            Boolean appointmentType,
            LocalDate dateFrom,
            LocalDate dateTo,
            Long therapistId,
            Long patientId,
            AppointmentStatus status,
            String searchTerm,
            Pageable pageable) {
        return appointmentRepository.findAppointmentsFiltered(
                appointmentType, dateFrom, dateTo, therapistId, patientId, status, searchTerm, pageable);
    }

    public Optional<Appointment> getAppointmentById(Long id) {
        return appointmentRepository.findById(id);
    }

    public List<Appointment> getAppointmentsForDate(Date date) {
        return appointmentRepository.findByDate(date);
    }

    public List<Appointment> getAppointmentsByCriteria(Long therapistId, Long patientId, Date date) {
        if (therapistId != null && patientId != null && date != null) {
            return appointmentRepository.findByTherapistIdAndPatientIdAndDate(therapistId, patientId, date);
        } else if (therapistId != null && date != null) {
            return appointmentRepository.findByTherapistIdAndDate(therapistId, date);
        } else if (patientId != null && date != null) {
            return appointmentRepository.findByPatientIdAndDate(patientId, date);
        } else if (date != null) {
            return appointmentRepository.findByDate(date);
        } else if (therapistId != null && patientId != null) {
            return appointmentRepository.findByTherapistIdAndPatientId(therapistId, patientId);
        } else if (therapistId != null) {
            return appointmentRepository.findByTherapistId(therapistId);
        } else if (patientId != null) {
            return appointmentRepository.findByPatientId(patientId);
        } else {
            return appointmentRepository.findAll();
        }
    }

    /**
     * Save appointment with integrated conflict check and audit logging.
     * Returns the saved appointment along with conflict information.
     */
    @Transactional
    public AppointmentSaveResult saveAppointmentWithConflictCheck(JSONAppointmentDTO appointmentDTO, boolean forceOnConflict) {
        // Support both nested therapist/patient objects and flat ID fields
        Long therapistId = appointmentDTO.getTherapistId() != null
                ? appointmentDTO.getTherapistId()
                : (appointmentDTO.getTherapist() != null ? appointmentDTO.getTherapist().getId() : null);
        Long patientId = appointmentDTO.getPatientId() != null
                ? appointmentDTO.getPatientId()
                : (appointmentDTO.getPatient() != null ? appointmentDTO.getPatient().getId() : null);

        if (therapistId == null) {
            throw new IllegalArgumentException("Therapist ID is required");
        }
        if (patientId == null) {
            throw new IllegalArgumentException("Patient ID is required");
        }

        Therapist therapist = therapistRepository.findById(therapistId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID"));
        Patient patient = patientRepository.findById(patientId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID"));

        LocalDate date = appointmentDTO.getDate() != null ? dateToLocalDate(appointmentDTO.getDate()) : null;
        LocalDateTime startTime = appointmentDTO.getStartTime() != null ? dateToLocalDateTime(appointmentDTO.getStartTime()) : null;
        LocalDateTime endTime = appointmentDTO.getEndTime() != null ? dateToLocalDateTime(appointmentDTO.getEndTime()) : null;

        // Build draft for conflict check
        AppointmentDraftDTO draft = AppointmentDraftDTO.builder()
                .id(appointmentDTO.getId())
                .therapistId(therapistId)
                .patientId(patientId)
                .date(date)
                .startTime(startTime)
                .endTime(endTime)
                .build();

        // Check conflicts
        ConflictCheckDTO conflictResult = conflictService.checkConflicts(draft);

        if (conflictResult.isHasConflict() && !forceOnConflict) {
            return new AppointmentSaveResult(null, conflictResult, false);
        }

        // Prepare entity
        Appointment appointment = new Appointment();
        boolean isUpdate = appointmentDTO.getId() != null;
        String beforeJson = null;

        if (isUpdate) {
            appointment = appointmentRepository.findById(appointmentDTO.getId())
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));
            beforeJson = auditService.toAuditJson(appointment);
        }

        appointment.setTherapist(therapist);
        appointment.setPatient(patient);
        appointment.setDate(date);
        appointment.setStartTime(startTime);
        appointment.setEndTime(endTime);
        appointment.setComment(appointmentDTO.getComment());
        // Only update createdBySeriesAppointment if explicitly provided - preserve existing value on update
        if (!isUpdate || appointmentDTO.getCreatedBySeriesAppointment() != null) {
            appointment.setCreatedBySeriesAppointment(appointmentDTO.getCreatedBySeriesAppointment());
        }
        // Note: appointmentSeries relationship is NOT touched here - it remains linked to the series
        appointment.setIsElectric(appointmentDTO.getIsElectric());
        appointment.setIsHotair(appointmentDTO.getIsHotair());
        appointment.setIsUltrasonic(appointmentDTO.getIsUltrasonic());
        appointment.setStatus(AppointmentStatus.SCHEDULED);

        Appointment saved = appointmentRepository.save(appointment);

        // Audit logging
        auditService.record(AuditService.builder()
                .actor(getCurrentUserId(), getCurrentUsername())
                .entity(AuditEntityType.APPOINTMENT, saved.getId())
                .action(isUpdate ? AuditAction.UPDATE : AuditAction.CREATE)
                .before(beforeJson)
                .after(auditService.toAuditJson(saved)));

        return new AppointmentSaveResult(saved, conflictResult, true);
    }

    /**
     * Legacy save method (calls new method with forceOnConflict=true for backward compatibility)
     */
    @Transactional
    public Appointment saveAppointment(JSONAppointmentDTO appointmentDTO) {
        AppointmentSaveResult result = saveAppointmentWithConflictCheck(appointmentDTO, true);
        return result.getAppointment();
    }

    /**
     * Move appointment (Drag & Drop support).
     * Changes date, startTime, endTime and optionally therapist.
     */
    @Transactional
    public AppointmentSaveResult moveAppointment(Long appointmentId, LocalDate newDate,
            LocalDateTime newStartTime, LocalDateTime newEndTime, Long newTherapistId, boolean forceOnConflict) {

        Appointment appointment = appointmentRepository.findById(appointmentId)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found: " + appointmentId));

        String beforeJson = auditService.toAuditJson(appointment);

        Long therapistId = newTherapistId != null ? newTherapistId : appointment.getTherapist().getId();

        // Build draft for conflict check
        AppointmentDraftDTO draft = AppointmentDraftDTO.builder()
                .id(appointmentId)
                .therapistId(therapistId)
                .patientId(appointment.getPatient().getId())
                .date(newDate)
                .startTime(newStartTime)
                .endTime(newEndTime)
                .build();

        ConflictCheckDTO conflictResult = conflictService.checkConflicts(draft);

        if (conflictResult.isHasConflict() && !forceOnConflict) {
            return new AppointmentSaveResult(null, conflictResult, false);
        }

        // Apply changes
        if (newTherapistId != null && !newTherapistId.equals(appointment.getTherapist().getId())) {
            Therapist newTherapist = therapistRepository.findById(newTherapistId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID"));
            appointment.setTherapist(newTherapist);
        }

        appointment.setDate(newDate);
        appointment.setStartTime(newStartTime);
        appointment.setEndTime(newEndTime);

        Appointment saved = appointmentRepository.save(appointment);

        // Audit logging
        auditService.record(AuditService.builder()
                .actor(getCurrentUserId(), getCurrentUsername())
                .entity(AuditEntityType.APPOINTMENT, appointmentId)
                .action(AuditAction.UPDATE)
                .before(beforeJson)
                .after(auditService.toAuditJson(saved)));

        return new AppointmentSaveResult(saved, conflictResult, true);
    }

    /**
     * Cancel appointment (soft delete).
     */
    @Transactional
    public Appointment cancelAppointment(Long id, String reason) {
        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

        String beforeJson = auditService.toAuditJson(appointment);

        appointment.setStatus(AppointmentStatus.CANCELLED);
        if (reason != null && !reason.isEmpty()) {
            appointment.setComment((appointment.getComment() != null ? appointment.getComment() + " | " : "")
                    + "Storniert: " + reason);
        }

        Appointment saved = appointmentRepository.save(appointment);

        // Audit logging
        auditService.record(AuditService.builder()
                .actor(getCurrentUserId(), getCurrentUsername())
                .entity(AuditEntityType.APPOINTMENT, id)
                .action(AuditAction.CANCEL)
                .before(beforeJson)
                .after(auditService.toAuditJson(saved)));

        return saved;
    }

    /**
     * Get conflicts for a draft appointment (used by frontend before saving).
     */
    public ConflictCheckDTO checkConflictsForDraft(AppointmentDraftDTO draft) {
        return conflictService.checkConflicts(draft);
    }

    private Long getCurrentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof User) {
            return ((User) auth.getPrincipal()).getId();
        }
        return null;
    }

    private String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            return auth.getName();
        }
        return "system";
    }


    public List<Appointment> findAvailableAppointments(Long therapistId, Long patientId, int timeOfDayId, Integer duration) {
        List<Appointment> availableAppointments = new ArrayList<>();
        List<Absence> absences = absenceService.getAbsencesByTherapistId(therapistId);

        // Datum von heute als Date-Objekt
        Date today = new Date();
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(today);

        LocalTime startTime = TimeOfDayService.getStartTime(timeOfDayId);
        LocalTime endTime = TimeOfDayService.getEndTime(timeOfDayId);

        while (startTime.plusMinutes(duration).isBefore(endTime)) {
            calendar.set(Calendar.HOUR_OF_DAY, startTime.getHour());
            calendar.set(Calendar.MINUTE, startTime.getMinute());
            Date startDateTime = calendar.getTime();

            calendar.add(Calendar.MINUTE, duration);
            Date endDateTime = calendar.getTime();

            if (isSlotAvailable(therapistId, startDateTime, endDateTime) && !isTherapistAbsent(absences, startDateTime, endDateTime)) {
                Appointment potentialAppointment = new Appointment();
                potentialAppointment.setTherapist(therapistRepository.findById(therapistId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid therapist ID")));

                potentialAppointment.setPatient(patientRepository.findById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Invalid patient ID")));
                potentialAppointment.setStartTime(dateToLocalDateTime(startDateTime));
                potentialAppointment.setEndTime(dateToLocalDateTime(endDateTime));
                potentialAppointment.setIsElectric(false);
                potentialAppointment.setIsHotair(false);
                potentialAppointment.setIsUltrasonic(false);
                potentialAppointment.setDate(dateToLocalDate(today));

                availableAppointments.add(potentialAppointment);
            }

            startTime = startTime.plusMinutes(duration);
        }

        return availableAppointments;
    }

    private boolean isTherapistAbsent(List<Absence> absences, Date startDateTime, Date endDateTime) {
        for (Absence absence : absences) {
            if (absence.getDate() != null) {
                // SPECIAL (one-time) absence
                LocalDateTime checkStart = dateToLocalDateTime(startDateTime);
                LocalDateTime checkEnd = dateToLocalDateTime(endDateTime);
                LocalDate absenceStartDate = absence.getDate();
                LocalDate absenceEndDate = absence.getEndDate() != null ? absence.getEndDate() : absenceStartDate;
                LocalDate checkDate = checkStart.toLocalDate();
                LocalDate checkEndDate = checkEnd.toLocalDate();

                // Check if appointment overlaps with absence date range
                if (!checkDate.isAfter(absenceEndDate) && !checkEndDate.isBefore(absenceStartDate)) {
                    // Appointment date overlaps with absence date range
                    LocalDateTime absenceStart = absence.getStartTime().atDate(absenceStartDate);
                    LocalDateTime absenceEnd = absence.getEndTime().atDate(absenceStartDate);

                    // If times are null, it's a full-day absence
                    if (absenceStart == null || absenceEnd == null) {
                        return true;
                    }

                    // Check time overlap
                    if (!checkStart.isAfter(absenceEnd) && !checkEnd.isBefore(absenceStart)) {
                        return true; // Zeitliche Überlappung gefunden
                    }
                }
            } else if (absence.getWeekday() != null && !absence.getWeekday().isEmpty() && matchesWeeklyAbsence(absence, startDateTime, endDateTime)) {
                return true; // Überlappung mit wöchentlicher Abwesenheit gefunden
            }
        }
        return false;
    }

    private boolean matchesWeeklyAbsence(Absence absence, Date start, Date end) {
        Calendar calStart = Calendar.getInstance();
        calStart.setTime(start);
        int startDayOfWeek = calStart.get(Calendar.DAY_OF_WEEK);

        Calendar calEnd = Calendar.getInstance();
        calEnd.setTime(end);
        int endDayOfWeek = calEnd.get(Calendar.DAY_OF_WEEK);

        int absenceDayOfWeek = convertWeekdayStringToIndex(absence.getWeekday());
        return absenceDayOfWeek == startDayOfWeek || absenceDayOfWeek == endDayOfWeek;
    }

    private int convertWeekdayStringToIndex(String weekday) {
        switch (weekday) {
            case "Sonntag" -> {
                return Calendar.SUNDAY;
            }
            case "Montag" -> {
                return Calendar.MONDAY;
            }
            case "Dienstag" -> {
                return Calendar.TUESDAY;
            }
            case "Mittwoch" -> {
                return Calendar.WEDNESDAY;
            }
            case "Donnerstag" -> {
                return Calendar.THURSDAY;
            }
            case "Freitag" -> {
                return Calendar.FRIDAY;
            }
            case "Samstag" -> {
                return Calendar.SATURDAY;
            }
            default -> throw new IllegalArgumentException("Unbekannter Wochentag: " + weekday); // Fehler werfen bei ungültigem Wochentag
        }
    }

    private boolean isSlotAvailable(Long therapistId, Date startDateTime, Date endDateTime) {
        // Prüfen, ob der Slot Überschneidungen mit bestehenden Terminen hat
        LocalDate date = dateToLocalDate(startDateTime);
        LocalDateTime checkStart = dateToLocalDateTime(startDateTime);
        LocalDateTime checkEnd = dateToLocalDateTime(endDateTime);
        return appointmentRepository.findAll().stream()
            .filter(a -> a.getDate().equals(date) && a.getTherapist().getId().equals(therapistId))
            .noneMatch(appointment -> checkStart.isBefore(appointment.getEndTime()) &&
                       checkEnd.isAfter(appointment.getStartTime()));
    }

    public boolean checkForConflicts(Appointment newAppointment) {
        // Holen Sie alle Termine
        List<Appointment> allAppointments = appointmentRepository.findAll();

        List<Appointment> todayAppointments = allAppointments.stream()
                .filter(appointment -> appointment.getDate().equals(newAppointment.getDate()))
                .collect(Collectors.toList());

        // Filtern Sie nach Therapeut
        List<Appointment> therapistAppointments = todayAppointments.stream()
                .filter(appointment -> appointment.getTherapist().getId().equals(newAppointment.getTherapist().getId()))
                .collect(Collectors.toList());

        // Überprüfen Sie auf Überlappungen
        for (Appointment existingAppointment : therapistAppointments) {
            if (isOverlapping(existingAppointment, newAppointment)) {
                return true;
            }
        }
        return false;
    }

    public boolean checkForConflicts(Appointment app1, Appointment app2) {
        // Überprüfe ob zwei Appointments Überlappungen erzeugten
        if (!app1.getDate().equals(app2.getDate()) ||
            !app1.getTherapist().getId().equals(app2.getTherapist().getId())) {
            return false;
        }
        return isOverlapping(app1, app2);
    }

    private boolean isOverlapping(Appointment existingAppointment, Appointment newAppointment) {
        return existingAppointment.getStartTime().isBefore(newAppointment.getEndTime()) &&
               existingAppointment.getEndTime().isAfter(newAppointment.getStartTime());
    }

    public Appointment convertDTOToEntity(JSONAppointmentDTO dto) {
        Appointment appointment = new Appointment();
        appointment.setId(dto.getId());
        appointment.setDate(dto.getDate() != null ? dateToLocalDate(dto.getDate()) : null);
        appointment.setComment(dto.getComment());
        appointment.setCreatedBySeriesAppointment(dto.getCreatedBySeriesAppointment());
        appointment.setStartTime(dto.getStartTime() != null ? dateToLocalDateTime(dto.getStartTime()) : null);
        appointment.setEndTime(dto.getEndTime() != null ? dateToLocalDateTime(dto.getEndTime()) : null);
        appointment.setIsElectric(dto.getIsElectric());
        appointment.setIsHotair(dto.getIsHotair());
        appointment.setIsUltrasonic(dto.getIsUltrasonic());
        appointment.setPatient(patientService.convertDTOToEntity(dto.getPatient()));
        appointment.setTherapist(therapistService.convertDTOToEntity(dto.getTherapist()));
        // Weitere Felder falls nötig
        return appointment;
    }

    private LocalDate dateToLocalDate(Date date) {
        return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDate();
    }

    private LocalDateTime dateToLocalDateTime(Date date) {
        return date.toInstant().atZone(ZoneId.of("UTC")).toLocalDateTime();
    }

    public List<Appointment> getAppointmentsWithConflicts() {
        List<Appointment> allAppointments = getAllAppointments();
        List<Appointment> conflictingAppointments = new ArrayList<>();

        for (int i = 0; i < allAppointments.size(); i++) {
            Appointment app1 = allAppointments.get(i);
            for (int j = i + 1; j < allAppointments.size(); j++) {
                Appointment app2 = allAppointments.get(j);
                if (checkForConflicts(app1, app2) && !conflictingAppointments.contains(app1)) {
                    conflictingAppointments.add(app1);
                }
                if (checkForConflicts(app1, app2) && !conflictingAppointments.contains(app2)) {
                    conflictingAppointments.add(app2);
                }
            }
        }
        return conflictingAppointments;
    }

    @Transactional
    public void deleteAppointment(Long id) {
        Appointment existing = appointmentRepository.findById(id).orElse(null);
        if (existing != null) {
            String beforeJson = auditService.toAuditJson(existing);

            // If this appointment was created by a series, add a cancellation record
            if (Boolean.TRUE.equals(existing.getCreatedBySeriesAppointment()) && existing.getAppointmentSeries() != null) {
                Cancellation cancellation = new Cancellation();
                cancellation.setAppointmentSeries(existing.getAppointmentSeries());
                cancellation.setDate(existing.getDate());
                cancellationRepository.save(cancellation);
            }

            appointmentRepository.deleteById(id);

            // Audit-Log
            auditService.record(AuditService.builder()
                    .actor(getCurrentUserId(), getCurrentUsername())
                    .entity(AuditEntityType.APPOINTMENT, id)
                    .action(AuditAction.DELETE)
                    .before(beforeJson));
        }
    }

    /**
     * Get appointments by therapist with optional date range.
     */
    public List<Appointment> getAppointmentsByTherapist(Long therapistId, LocalDate from, LocalDate to) {
        List<Appointment> appointments = appointmentRepository.findByTherapistId(therapistId);

        if (from != null || to != null) {
            appointments = appointments.stream()
                    .filter(a -> {
                        LocalDate appointmentDate = a.getDate();
                        boolean afterFrom = from == null || !appointmentDate.isBefore(from);
                        boolean beforeTo = to == null || !appointmentDate.isAfter(to);
                        return afterFrom && beforeTo;
                    })
                    .toList();
        }

        return appointments;
    }

    /**
     * Get appointments by patient with optional date range.
     */
    public List<Appointment> getAppointmentsByPatient(Long patientId, LocalDate from, LocalDate to) {
        List<Appointment> appointments = appointmentRepository.findByPatientId(patientId);

        if (from != null || to != null) {
            appointments = appointments.stream()
                    .filter(a -> {
                        LocalDate appointmentDate = a.getDate();
                        boolean afterFrom = from == null || !appointmentDate.isBefore(from);
                        boolean beforeTo = to == null || !appointmentDate.isAfter(to);
                        return afterFrom && beforeTo;
                    })
                    .toList();
        }

        return appointments;
    }

    /**
     * Get appointments in a date range.
     */
    public List<Appointment> getAppointmentsByDateRange(LocalDate from, LocalDate to) {
        return appointmentRepository.findAll().stream()
                .filter(a -> {
                    LocalDate appointmentDate = a.getDate();
                    return !appointmentDate.isBefore(from) && !appointmentDate.isAfter(to);
                })
                .toList();
    }
}
