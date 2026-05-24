package com.collectx.dunning.controller;

import com.collectx.dunning.dto.AttemptRequestDTO;
import com.collectx.dunning.dto.AttemptResponseDTO;
import com.collectx.dunning.service.DunningService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dunning")
@RequiredArgsConstructor
public class DunningController {

    private final DunningService service;

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'SUPERVISOR')")
    @PostMapping("/attempt")
    public AttemptResponseDTO attempt(@RequestBody AttemptRequestDTO req) {
        return service.makeAttempt(req);
    }


    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'COMPLIANCE')")
    @GetMapping("/attempts")
    public List<AttemptResponseDTO> getAll() {
        return service.getAllAttempts();
    }


    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'SUPERVISOR', 'COMPLIANCE')")
    @GetMapping("/attempts/loan/{loanId}")
    public List<AttemptResponseDTO> getByLoan(@PathVariable Long loanId) {
        return service.getAttemptsByLoan(loanId);
    }
}
