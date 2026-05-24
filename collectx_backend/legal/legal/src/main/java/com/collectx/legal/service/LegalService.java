package com.collectx.legal.service;

import com.collectx.legal.dto.*;
import com.collectx.legal.entity.LegalAction;
import com.collectx.legal.entity.Recovery;
import com.collectx.legal.entity.WriteOff;
import com.collectx.legal.enums.LegalStatus;
import com.collectx.legal.enums.RecoveryStatus;
import com.collectx.legal.enums.WriteOffStatus;
import com.collectx.legal.repository.LegalRepository;
import com.collectx.legal.repository.RecoveryRepository;
import com.collectx.legal.repository.WriteOffRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LegalService {

    private static final Logger log = LoggerFactory.getLogger(LegalService.class);

    private final LegalRepository legalRepo;
    private final WriteOffRepository writeOffRepo;
    private final RecoveryRepository recoveryRepo;

    // ── CREATE ────────────────────────────────────────────────────────────────

    public LegalActionResponseDTO createLegal(LegalActionRequestDTO dto) {
        log.info("Filing legal action type={} for loan={} customer={}", dto.getActionType(), dto.getLoanAccountId(), dto.getCustomerId());

        LegalAction action = new LegalAction();
        action.setLoanAccountId(dto.getLoanAccountId());
        action.setCustomerId(dto.getCustomerId());
        action.setActionType(dto.getActionType());   // plain String — no enum conversion
        action.setCaseNumber(dto.getCaseNumber());
        action.setCourtName(dto.getCourtName());
        action.setFiledDate(dto.getFiledDate() != null ? LocalDate.parse(dto.getFiledDate()) : null);
        action.setNotes(dto.getNotes());
        action.setStatus(dto.getStatus() != null ? LegalStatus.valueOf(dto.getStatus()) : LegalStatus.OPEN);

        LegalAction saved = legalRepo.save(action);
        log.info("Legal action filed with id={}", saved.getLegalId());
        return toLegalResponse(saved);
    }

    public WriteOffResponseDTO writeOff(WriteOffRequestDTO dto) {
        log.info("Recording write-off amount={} for loan={} customer={}", dto.getWriteOffAmount(), dto.getLoanAccountId(), dto.getCustomerId());

        WriteOff w = new WriteOff();
        w.setLoanAccountId(dto.getLoanAccountId());
        w.setCustomerId(dto.getCustomerId());
        w.setWriteOffAmount(dto.getWriteOffAmount());
        w.setReason(dto.getReason());
        w.setApprovedBy(dto.getApprovedBy());
        w.setStatus(dto.getStatus() != null ? WriteOffStatus.valueOf(dto.getStatus()) : WriteOffStatus.POSTED);

        WriteOff saved = writeOffRepo.save(w);
        log.info("Write-off recorded with id={}", saved.getWriteOffId());
        return toWriteOffResponse(saved);
    }

    public RecoveryResponseDTO recover(RecoveryRequestDTO dto) {
        log.info("Recording recovery amount={} type={} for loan={}", dto.getRecoveredAmount(), dto.getRecoveryType(), dto.getLoanAccountId());

        Recovery r = new Recovery();
        r.setLoanAccountId(dto.getLoanAccountId());
        r.setCustomerId(dto.getCustomerId());
        r.setRecoveredAmount(dto.getRecoveredAmount());
        r.setRecoveryType(dto.getRecoveryType());
        r.setRecoveryDate(dto.getRecoveryDate() != null ? LocalDate.parse(dto.getRecoveryDate()) : null);
        r.setNotes(dto.getNotes());
        r.setStatus(dto.getStatus() != null ? RecoveryStatus.valueOf(dto.getStatus()) : RecoveryStatus.PENDING);

        Recovery saved = recoveryRepo.save(r);
        log.info("Recovery recorded with id={}", saved.getRecoveryId());
        return toRecoveryResponse(saved);
    }

    // ── PAYMENT AUTO-TRIGGER ──────────────────────────────────────────────────

    public void onPayment(Long loanAccountId, Long customerId, Double amount) {
        log.info("Payment event received for loanId={} amount={}", loanAccountId, amount);

        // If write-off exists for this loan → auto-create Recovery
        writeOffRepo.findByLoanAccountId(loanAccountId).ifPresent(writeOff -> {
            Recovery r = new Recovery();
            r.setLoanAccountId(loanAccountId);
            r.setCustomerId(customerId);
            r.setRecoveredAmount(amount);
            r.setRecoveryType("PAYMENT");
            r.setRecoveryDate(LocalDate.now());
            r.setNotes("Auto-recovery triggered by payment");
            r.setStatus(RecoveryStatus.COMPLETED);
            recoveryRepo.save(r);
            log.info("Auto-recovery created for loanId={} amount={}", loanAccountId, amount);
        });
    }

    // ── UPDATE STATUS ─────────────────────────────────────────────────────────

    public LegalActionResponseDTO updateActionStatus(Long id, String status) {
        LegalAction action = legalRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Legal action not found: " + id));
        action.setStatus(LegalStatus.valueOf(status));
        log.info("Legal action id={} status updated to {}", id, status);
        return toLegalResponse(legalRepo.save(action));
    }

    public WriteOffResponseDTO updateWriteOffStatus(Long id, String status) {
        WriteOff w = writeOffRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Write-off not found: " + id));
        w.setStatus(WriteOffStatus.valueOf(status));
        log.info("Write-off id={} status updated to {}", id, status);
        return toWriteOffResponse(writeOffRepo.save(w));
    }

    public RecoveryResponseDTO updateRecoveryStatus(Long id, String status) {
        Recovery r = recoveryRepo.findById(id)
                .orElseThrow(() -> new RuntimeException("Recovery not found: " + id));
        r.setStatus(RecoveryStatus.valueOf(status));
        log.info("Recovery id={} status updated to {}", id, status);
        return toRecoveryResponse(recoveryRepo.save(r));
    }

    // ── GET ALL ───────────────────────────────────────────────────────────────

    public List<LegalActionResponseDTO> getAllActions() {
        log.debug("Fetching all legal actions");
        return legalRepo.findAll().stream().map(this::toLegalResponse).collect(Collectors.toList());
    }

    public List<WriteOffResponseDTO> getAllWriteOffs() {
        log.debug("Fetching all write-offs");
        return writeOffRepo.findAll().stream().map(this::toWriteOffResponse).collect(Collectors.toList());
    }

    public List<RecoveryResponseDTO> getAllRecoveries() {
        log.debug("Fetching all recoveries");
        return recoveryRepo.findAll().stream().map(this::toRecoveryResponse).collect(Collectors.toList());
    }

    // ── MAPPERS ───────────────────────────────────────────────────────────────

    private LegalActionResponseDTO toLegalResponse(LegalAction a) {
        return LegalActionResponseDTO.builder()
                .legalActionId(a.getLegalId())   // legalId is the real PK column
                .loanAccountId(a.getLoanAccountId())
                .customerId(a.getCustomerId())
                .actionType(a.getActionType())
                .caseNumber(a.getCaseNumber())
                .courtName(a.getCourtName())
                .filedDate(a.getFiledDate() != null ? a.getFiledDate().toString() : null)
                .status(a.getStatus() != null ? a.getStatus().name() : null)
                .notes(a.getNotes())
                .build();
    }

    private WriteOffResponseDTO toWriteOffResponse(WriteOff w) {
        return WriteOffResponseDTO.builder()
                .writeOffId(w.getWriteOffId())
                .loanAccountId(w.getLoanAccountId())
                .customerId(w.getCustomerId())
                .writeOffAmount(w.getWriteOffAmount())
                .reason(w.getReason())
                .approvedBy(w.getApprovedBy())
                .status(w.getStatus() != null ? w.getStatus().name() : null)
                .build();
    }

    private RecoveryResponseDTO toRecoveryResponse(Recovery r) {
        return RecoveryResponseDTO.builder()
                .recoveryId(r.getRecoveryId())
                .loanAccountId(r.getLoanAccountId())
                .customerId(r.getCustomerId())
                .recoveredAmount(r.getRecoveredAmount())
                .recoveryType(r.getRecoveryType())
                .recoveryDate(r.getRecoveryDate() != null ? r.getRecoveryDate().toString() : null)
                .notes(r.getNotes())
                .build();
    }
}
