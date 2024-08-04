package com.example.physiokalendar.service;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.example.physiokalendar.entity.Cancellation;
import com.example.physiokalendar.repository.CancellationRepository;

@Service
public class CancellationService {

    @Autowired
    private CancellationRepository cancellationRepository;

    public List<Cancellation> getAllCancellations() {
        return cancellationRepository.findAll();
    }

    public Optional<Cancellation> getCancellationById(Long id) {
        return cancellationRepository.findById(id);
    }

    public Cancellation saveCancellation(Cancellation cancellation) {
        return cancellationRepository.save(cancellation);
    }

    public void deleteCancellation(Long id) {
        cancellationRepository.deleteById(id);
    }
}
