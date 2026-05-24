package com.collectx.payment.controller;

import com.collectx.payment.dto.*;
import com.collectx.payment.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService service;


    @PreAuthorize("isAuthenticated()")
    @PostMapping("/ptp")
    public PTPResponseDTO createPTP(@RequestBody PTPRequestDTO dto) {
        return service.createPTP(dto);
    }


    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'ADMIN')")
    @PostMapping("/create")
    public PaymentResponseDTO pay(@RequestBody PaymentRequestDTO dto,
                                  @RequestHeader("Authorization") String token) {
        return service.makePayment(dto, token);
    }

    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN')")
    @PostMapping("/settlement")
    public SettlementResponseDTO settlement(@RequestBody SettlementRequestDTO dto) {
        return service.requestSettlement(dto);
    }


    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'COMPLIANCE', 'ADMIN', 'RECOVERY', 'FIELD')")
    @GetMapping("/ptp/all")
    public List<PTPResponseDTO> getAllPTPs() {
        return service.getAllPTPs();
    }


    @PreAuthorize("isAuthenticated()")
    @GetMapping("/payments")
    public List<PaymentResponseDTO> getAllPayments() {
        return service.getAllPayments();
    }


    @PreAuthorize("hasAnyRole('SUPERVISOR', 'ADMIN', 'COMPLIANCE')")
    @GetMapping("/settlements")
    public List<SettlementResponseDTO> getAllSettlements() {
        return service.getAllSettlements();
    }


    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'COMPLIANCE', 'ADMIN', 'RECOVERY', 'FIELD')")
    @GetMapping("/ptp/{loanId}")
    public List<PTPResponseDTO> getPTP(@PathVariable Long loanId) {
        return service.getPTPs(loanId);
    }

    @PreAuthorize("hasAnyRole('AGENT', 'SUPERVISOR', 'COMPLIANCE', 'ADMIN', 'RECOVERY', 'FIELD')")
    @GetMapping("/ptp/loan/{loanId}")
    public List<PTPResponseDTO> getPTPByLoan(@PathVariable Long loanId) {
        return service.getPTPs(loanId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @PatchMapping("/settlement/{id}/approve")
    public SettlementResponseDTO approveSettlement(@PathVariable Long id,
                                                   @RequestParam String decision) {
        return service.approveSettlement(id, decision);
    }
}
