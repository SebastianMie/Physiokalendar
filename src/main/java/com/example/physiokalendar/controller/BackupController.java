package com.example.physiokalendar.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import com.example.physiokalendar.service.BackupService;

@RestController
@RequestMapping("/api/admin/backup")
@PreAuthorize("hasRole('ADMIN')")
public class BackupController {

    private final BackupService backupService;

    public BackupController(BackupService backupService) {
        this.backupService = backupService;
    }

    @PostMapping("/create")
    public ResponseEntity<Map<String, Object>> createBackup(
            @RequestParam(value = "type", required = false, defaultValue = "auto") String backupType) {
        Map<String, Object> response = new HashMap<>();
        try {
            // Validate backup type
            if (!backupType.matches("^(full|incremental|auto)$")) {
                response.put("success", false);
                response.put("message", "Ungültiger Backup-Typ. Erlaubt sind: full, incremental, auto");
                return ResponseEntity.badRequest().body(response);
            }

            String backupPath = backupService.createBackup(backupType);
            if (backupPath != null) {
                response.put("success", true);
                response.put("message", "Backup erfolgreich erstellt (" + backupType + ")");
                response.put("path", backupPath);
                response.put("type", backupType);
                return ResponseEntity.ok(response);
            } else {
                response.put("success", false);
                response.put("message", "Fehler beim Erstellen des Backups - bitte überprüfen Sie die Server-Logs");
                return ResponseEntity.status(500).body(response);
            }
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Fehler beim Erstellen des Backups: " + e.getMessage());
            return ResponseEntity.status(500).body(response);
        }
    }

    @GetMapping("/list")
    public ResponseEntity<List<Map<String, Object>>> listBackups() {
        return ResponseEntity.ok(backupService.getBackupFiles());
    }

    @DeleteMapping("/delete/{filename}")
    public ResponseEntity<Map<String, Object>> deleteBackup(@PathVariable String filename) {
        Map<String, Object> response = new HashMap<>();

        if (backupService.deleteBackup(filename)) {
            response.put("success", true);
            response.put("message", "Backup erfolgreich gelöscht");
            return ResponseEntity.ok(response);
        } else {
            response.put("success", false);
            response.put("message", "Fehler beim Löschen des Backups");
            return ResponseEntity.status(500).body(response);
        }
    }
}
