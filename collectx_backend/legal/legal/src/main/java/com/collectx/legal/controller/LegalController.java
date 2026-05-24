package com.collectx.legal.controller;

import com.collectx.legal.dto.*;
import com.collectx.legal.service.LegalService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/legal")
@RequiredArgsConstructor
public class LegalController {

    private final LegalService service;

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('COMPLIANCE', 'ADMIN')")
    @PostMapping("/action")
    public LegalActionResponseDTO legal(@RequestBody LegalActionRequestDTO dto) {
        return service.createLegal(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE')")
    @PostMapping("/writeoff")
    public WriteOffResponseDTO writeOff(@RequestBody WriteOffRequestDTO dto) {
        return service.writeOff(dto);
    }

    @PreAuthorize("hasAnyRole('RECOVERY', 'ADMIN', 'COMPLIANCE')")
    @PostMapping("/recovery")
    public RecoveryResponseDTO recovery(@RequestBody RecoveryRequestDTO dto) {
        return service.recover(dto);
    }

    // ── UPDATE STATUS ─────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('COMPLIANCE', 'ADMIN')")
    @PatchMapping("/action/{id}/status")
    public LegalActionResponseDTO updateActionStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateActionStatus(id, status);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE')")
    @PatchMapping("/writeoff/{id}/status")
    public WriteOffResponseDTO updateWriteOffStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateWriteOffStatus(id, status);
    }

    @PreAuthorize("hasAnyRole('RECOVERY', 'ADMIN', 'COMPLIANCE')")
    @PatchMapping("/recovery/{id}/status")
    public RecoveryResponseDTO updateRecoveryStatus(@PathVariable Long id, @RequestParam String status) {
        return service.updateRecoveryStatus(id, status);
    }

    // ── PAYMENT AUTO-TRIGGER (called by Payment Service via Feign) ───────────
    @PreAuthorize("isAuthenticated()")
    @PostMapping("/on-payment")
    public void onPayment(@RequestBody Map<String, Object> body) {
        Long loanAccountId = Long.valueOf(body.get("loanAccountId").toString());
        Long customerId    = body.get("customerId") != null
                             ? Long.valueOf(body.get("customerId").toString()) : null;
        Double amount      = Double.valueOf(body.get("amount").toString());
        service.onPayment(loanAccountId, customerId, amount);
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/actions")
    public List<LegalActionResponseDTO> getAllActions() {
        return service.getAllActions();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE')")
    @GetMapping("/writeoffs")
    public List<WriteOffResponseDTO> getAllWriteOffs() {
        return service.getAllWriteOffs();
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'RECOVERY', 'COMPLIANCE')")
    @GetMapping("/recoveries")
    public List<RecoveryResponseDTO> getAllRecoveries() {
        return service.getAllRecoveries();
    }
}
