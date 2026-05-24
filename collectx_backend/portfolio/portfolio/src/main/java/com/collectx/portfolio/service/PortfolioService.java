package com.collectx.portfolio.service;

import com.collectx.portfolio.dto.LoanRequestDTO;
import com.collectx.portfolio.dto.LoanResponseDTO;
import com.collectx.portfolio.entity.LoanRef;
import com.collectx.portfolio.feign.CustomerClient;
import com.collectx.portfolio.feign.StrategyClient;
import com.collectx.portfolio.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PortfolioService {

    private static final Logger log = LoggerFactory.getLogger(PortfolioService.class);

    private final StrategyClient strategyClient;
    private final CustomerClient customerClient;
    private final LoanRepository loanRepository;

    // ── DTO → Entity mapping ──────────────────────────────────────────────────
    private LoanRef toEntity(LoanRequestDTO dto) {
        LoanRef loan = new LoanRef();
        loan.setCustomerId(dto.getCustomerId());
        loan.setProduct(dto.getProduct());
        loan.setPrincipalOS(dto.getPrincipalOS());
        loan.setInterestOS(dto.getInterestOS() != null ? dto.getInterestOS() : 0.0);
        loan.setLastPaymentDate(LocalDate.parse(dto.getLastPaymentDate()));
        loan.setRegion(dto.getRegion());
        return loan;
    }

    // ── Entity → DTO mapping ──────────────────────────────────────────────────
    private LoanResponseDTO toDTO(LoanRef loan) {
        LoanResponseDTO dto = new LoanResponseDTO();
        dto.setLoanAccountId(loan.getLoanAccountId());
        dto.setCustomerId(loan.getCustomerId());
        dto.setProduct(loan.getProduct());
        dto.setPrincipalOS(loan.getPrincipalOS());
        dto.setInterestOS(loan.getInterestOS());
        dto.setDpd(loan.getDpd());
        dto.setBucket(loan.getBucket());
        dto.setLastPaymentDate(loan.getLastPaymentDate() != null
                ? loan.getLastPaymentDate().toString() : null);
        dto.setRegion(loan.getRegion());
        dto.setStatus(loan.getStatus());
        dto.setWriteOffDate(loan.getWriteOffDate() != null
                ? loan.getWriteOffDate().toString() : null);
        dto.setWriteOffReason(loan.getWriteOffReason());
        return dto;
    }

    // ─────────────────────────────────────────────────────────────────────────

    public LoanResponseDTO createLoan(LoanRequestDTO dto, String token) {
        log.info("Creating loan for customerId={} product={}", dto.getCustomerId(), dto.getProduct());
        LoanRef loan = toEntity(dto);

        // Validate customer exists
        if (loan.getCustomerId() != null) {
            Map<String, Object> customer = customerClient.getById(loan.getCustomerId());
            boolean isFallback = Boolean.TRUE.equals(customer.get("_fallback"));
            if (!isFallback && customer.get("customerId") == null) {
                throw new RuntimeException("Customer not found with ID: " + loan.getCustomerId());
            }
        }

        // Calculate DPD
        long dpd = ChronoUnit.DAYS.between(loan.getLastPaymentDate(), LocalDate.now());
        loan.setDpd((int) dpd);

        // Bucket logic
        if      (dpd <= 30) loan.setBucket("0-30");
        else if (dpd <= 60) loan.setBucket("31-60");
        else if (dpd <= 90) loan.setBucket("61-90");
        else                 loan.setBucket("90+");

        loan.setStatus(dpd > 0 ? "Delinquent" : "Current");

        LoanRef savedLoan = loanRepository.save(loan);
        log.info("Loan created loanAccountId={} dpd={} bucket={}", savedLoan.getLoanAccountId(), savedLoan.getDpd(), savedLoan.getBucket());

        // Trigger strategy auto-assignment
        String riskBand = "ALL";
        Map<String, String> body = new HashMap<>();
        body.put("loanAccountId", savedLoan.getLoanAccountId().toString());
        body.put("bucket", savedLoan.getBucket());
        body.put("riskBand", riskBand);
        log.info("Triggering strategy assignment for loanId={} bucket={} riskBand={}", savedLoan.getLoanAccountId(), savedLoan.getBucket(), riskBand);
        strategyClient.assignLoan(body);

        return toDTO(savedLoan);
    }

    public List<LoanResponseDTO> getAllLoans() {
        return loanRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public LoanResponseDTO getLoan(Long id) {
        LoanRef loan = loanRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Loan not found"));
        return toDTO(loan);
    }

    public void applyPayment(Long loanId, Double amount) {
        LoanRef loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found"));

        double remaining = amount;

        // Deduct from Interest first
        if (loan.getInterestOS() != null && loan.getInterestOS() > 0) {
            if (remaining >= loan.getInterestOS()) {
                remaining -= loan.getInterestOS();
                loan.setInterestOS(0.0);
            } else {
                loan.setInterestOS(loan.getInterestOS() - remaining);
                remaining = 0;
            }
        }

        // Then from Principal
        if (remaining > 0 && loan.getPrincipalOS() != null && loan.getPrincipalOS() > 0) {
            if (remaining >= loan.getPrincipalOS()) {
                loan.setPrincipalOS(0.0);
            } else {
                loan.setPrincipalOS(loan.getPrincipalOS() - remaining);
            }
        }

        if (loan.getPrincipalOS() == 0 && loan.getInterestOS() == 0) {
            loan.setStatus("CLOSED");
            loan.setDpd(0);
            loan.setBucket("0-30");
        } else {
            long dpd = ChronoUnit.DAYS.between(loan.getLastPaymentDate(), LocalDate.now());
            loan.setDpd((int) dpd);
            if      (dpd <= 30) loan.setBucket("0-30");
            else if (dpd <= 60) loan.setBucket("31-60");
            else if (dpd <= 90) loan.setBucket("61-90");
            else                 loan.setBucket("90+");
        }

        loanRepository.save(loan);
    }

    // ── Write-off loan ────────────────────────────────────────────────────────
    public LoanResponseDTO writeOffLoan(Long loanId, String reason) {
        LoanRef loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));

        if ("WRITTEN_OFF".equals(loan.getStatus())) {
            throw new RuntimeException("Loan is already written off");
        }

        loan.setStatus("WRITTEN_OFF");
        loan.setWriteOffDate(LocalDate.now());
        loan.setWriteOffReason(reason != null ? reason : "No reason provided");

        log.info("Loan written off loanId={} reason={}", loanId, reason);
        return toDTO(loanRepository.save(loan));
    }

    // ── Delete loan ───────────────────────────────────────────────────────────
    public void deleteLoan(Long loanId) {
        LoanRef loan = loanRepository.findById(loanId)
                .orElseThrow(() -> new RuntimeException("Loan not found: " + loanId));
        loanRepository.delete(loan);
        log.info("Loan deleted loanId={}", loanId);
    }
}
