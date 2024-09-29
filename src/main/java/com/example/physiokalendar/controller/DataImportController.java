package com.example.physiokalendar.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.physiokalendar.dataimport.DataImportService;

@RestController
@RequestMapping("/api/dataimport")
public class DataImportController {

    @Autowired
    private DataImportService dataImportService;

    @PostMapping("/import")
    public String importData(@RequestParam String filePath) {
        dataImportService.importData(filePath);
        return "Import abgeschlossen";
    }
}
