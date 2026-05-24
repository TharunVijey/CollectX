package com.collectx.portfolio.controller;

import com.collectx.portfolio.dto.LoanRequestDTO;
import com.collectx.portfolio.dto.LoanResponseDTO;
import com.collectx.portfolio.service.PortfolioService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService service;

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR')")
    @PostMapping("/loan")
    public LoanResponseDTO createLoan(@RequestBody LoanRequestDTO dto,
                                      @RequestHeader("Authorization") String token) {
        return service.createLoan(dto, token);
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/loans")
    public List<LoanResponseDTO> getAll() {
        return service.getAllLoans();
    }

    @PreAuthorize("isAuthenticated()")
    @GetMapping("/loan/{id}")
    public LoanResponseDTO getOne(@PathVariable Long id) {
        return service.getLoan(id);
    }

    @PreAuthorize("isAuthenticated()")
    @PutMapping("/loan/payment")
    public String applyPayment(@RequestBody Map<String, Object> req) {
        Long loanId = Long.parseLong(req.get("loanAccountId").toString());
        Double amount = Double.parseDouble(req.get("amount").toString());
        service.applyPayment(loanId, amount);
        return "Payment Applied Successfully";
    }

    // Write-off — ADMIN, SUPERVISOR, RECOVERY roles
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'RECOVERY')")
    @PatchMapping("/loan/{id}/writeoff")
    public LoanResponseDTO writeOffLoan(@PathVariable Long id,
                                        @RequestBody Map<String, String> req) {
        String reason = req.get("reason");
        return service.writeOffLoan(id, reason);
    }

    // Delete — ADMIN only
    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/loan/{id}")
    public String deleteLoan(@PathVariable Long id) {
        service.deleteLoan(id);
        return "Loan deleted successfully";
    }
}
