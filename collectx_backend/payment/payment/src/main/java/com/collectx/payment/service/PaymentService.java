package com.collectx.payment.service;

import com.collectx.payment.dto.*;
import com.collectx.payment.entity.PTP;
import com.collectx.payment.entity.PaymentRef;
import com.collectx.payment.entity.Settlement;
import com.collectx.payment.enums.ApprovalStatus;
import com.collectx.payment.enums.PTPStatus;
import com.collectx.payment.enums.PaymentStatus;
import com.collectx.payment.enums.SettlementStatus;
import com.collectx.payment.feign.LegalClient;
import com.collectx.payment.feign.NotificationClient;
import com.collectx.payment.feign.PortfolioClient;
import com.collectx.payment.feign.ReportingClient;
import com.collectx.payment.repository.PTPRepository;
import com.collectx.payment.repository.PaymentRepository;
import com.collectx.payment.repository.SettlementRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentService.class);

    private final PTPRepository ptpRepo;
    private final PaymentRepository paymentRepo;
    private final SettlementRepository settlementRepo;
    private final NotificationClient notificationClient;
    private final ReportingClient reportingClient;
    private final PortfolioClient portfolioClient;
    private final LegalClient legalClient;



    public PTPResponseDTO createPTP(PTPRequestDTO dto) {
        log.info("Creating PTP for loan={} customer={} amount={}", dto.getLoanAccountId(), dto.getCustomerId(), dto.getPromisedAmount());

        PTP ptp = new PTP();
        ptp.setLoanAccountId(dto.getLoanAccountId());
        ptp.setAgentId(dto.getAgentId());
        ptp.setCustomerId(dto.getCustomerId());
        ptp.setPromisedAmount(dto.getPromisedAmount());
        ptp.setPromisedDate(dto.getPromisedDate() != null ? LocalDate.parse(dto.getPromisedDate()) : null);
        ptp.setChannel(dto.getChannel());
        ptp.setPromisedBy(dto.getPromisedBy());
        ptp.setStatus(PTPStatus.OPEN);

        PTP saved = ptpRepo.save(ptp);
        log.info("PTP created with id={}", saved.getPtpId());
        return toPTPResponse(saved);
    }



    public PaymentResponseDTO makePayment(PaymentRequestDTO dto, String token) {
        log.info("Recording payment amount={} mode={} for loan={}", dto.getAmount(), dto.getPaymentMode(), dto.getLoanAccountId());

        PaymentRef payment = new PaymentRef();
        payment.setLoanAccountId(dto.getLoanAccountId());
        payment.setAgentId(dto.getAgentId());
        payment.setCustomerId(dto.getCustomerId());
        payment.setAmount(dto.getAmount());
        payment.setPaymentMode(dto.getPaymentMode());
        payment.setReferenceNumber(dto.getReferenceNumber());
        payment.setPaymentDate(LocalDate.now());
        payment.setStatus(PaymentStatus.POSTED);

        PaymentRef saved = paymentRepo.save(payment);
        log.info("Payment recorded with id={}", saved.getPaymentId());


        try {
            Map<String, Object> body = new HashMap<>();
            body.put("loanAccountId", dto.getLoanAccountId());
            body.put("amount", dto.getAmount());
            portfolioClient.applyPayment(body);
        } catch (Exception e) {
            log.warn("Portfolio balance update failed for loan={} — {}", dto.getLoanAccountId(), e.getMessage());
        }


        List<PTP> ptps = ptpRepo.findByLoanAccountId(dto.getLoanAccountId());
        for (PTP ptp : ptps) {
            if (ptp.getStatus() == PTPStatus.OPEN) {
                if (dto.getAmount() >= ptp.getPromisedAmount()) {
                    ptp.setStatus(PTPStatus.KEPT);
                    log.info("PTP id={} marked KEPT for loan={}", ptp.getPtpId(), dto.getLoanAccountId());

                    sendNotification(dto.getCustomerId(), dto.getLoanAccountId(),
                            "PTP KEPT for Loan " + dto.getLoanAccountId());
                } else {
                    ptp.setStatus(PTPStatus.BROKEN);
                    log.info("PTP id={} marked BROKEN for loan={}", ptp.getPtpId(), dto.getLoanAccountId());

                    sendNotification(dto.getCustomerId(), dto.getLoanAccountId(),
                            "PTP BROKEN for Loan " + dto.getLoanAccountId());
                }
                ptpRepo.save(ptp);
            }
        }

        try {
            Map<String, Object> reportBody = new HashMap<>();
            reportBody.put("agentId", dto.getAgentId() != null ? dto.getAgentId() : 1);
            reportBody.put("period", "MONTHLY");
            reportBody.put("accountsWorked", 1);
            reportBody.put("contactsMade", 1);
            reportBody.put("ptpsBooked", 1);
            reportBody.put("ptpKept", 1);
            reportBody.put("amountCollected", dto.getAmount());
            reportingClient.sendPerformance(reportBody);
        } catch (Exception e) {
            log.warn("Reporting failed for payment id={} — {}", saved.getPaymentId(), e.getMessage());
        }


        try {
            Map<String, Object> legalBody = new HashMap<>();
            legalBody.put("loanAccountId", dto.getLoanAccountId());
            legalBody.put("customerId",    dto.getCustomerId());
            legalBody.put("amount",        dto.getAmount());
            legalClient.onPayment(legalBody);
        } catch (Exception e) {
            log.warn("Legal automation failed for loan={} — {}", dto.getLoanAccountId(), e.getMessage());
        }

        return toPaymentResponse(saved);
    }



    public SettlementResponseDTO requestSettlement(SettlementRequestDTO dto) {
        log.info("Settlement request amount={} waiver={} for loan={} customer={}", dto.getSettlementAmount(), dto.getWaiverAmount(), dto.getLoanAccountId(), dto.getCustomerId());

        Settlement s = new Settlement();
        s.setLoanAccountId(dto.getLoanAccountId());
        s.setAgentId(dto.getAgentId());
        s.setCustomerId(dto.getCustomerId());
        s.setSettlementAmount(dto.getSettlementAmount());
        s.setWaiverAmount(dto.getWaiverAmount());
        s.setReason(dto.getReason());
        s.setApprovalStatus(ApprovalStatus.REQUESTED);
        s.setStatus(SettlementStatus.ACTIVE);

        Settlement saved = settlementRepo.save(s);
        log.info("Settlement created with id={}", saved.getSettlementId());
        return toSettlementResponse(saved);
    }


    public SettlementResponseDTO approveSettlement(Long settlementId, String decision) {
        Settlement s = settlementRepo.findById(settlementId)
                .orElseThrow(() -> new RuntimeException("Settlement not found: " + settlementId));

        if (s.getApprovalStatus() != ApprovalStatus.REQUESTED) {
            throw new RuntimeException("Settlement has already been " + s.getApprovalStatus().name().toLowerCase());
        }


        if ("APPROVED".equalsIgnoreCase(decision)) {
            s.setApprovalStatus(ApprovalStatus.APPROVED);
            s.setStatus(SettlementStatus.HONORED);   // settlement is now active/honored
            log.info("Settlement id={} APPROVED", settlementId);
        } else if ("REJECTED".equalsIgnoreCase(decision)) {
            s.setApprovalStatus(ApprovalStatus.REJECTED);
            s.setStatus(SettlementStatus.DEFAULTED);  // settlement rejected = defaulted
            log.info("Settlement id={} REJECTED", settlementId);
        } else {
            throw new RuntimeException("Invalid decision: " + decision + ". Use APPROVED or REJECTED");
        }

        return toSettlementResponse(settlementRepo.save(s));
    }



    public List<PTPResponseDTO> getAllPTPs() {
        log.debug("Fetching all PTPs");
        return ptpRepo.findAll().stream().map(this::toPTPResponse).collect(Collectors.toList());
    }

    public List<PaymentResponseDTO> getAllPayments() {
        log.debug("Fetching all payments");
        return paymentRepo.findAll().stream().map(this::toPaymentResponse).collect(Collectors.toList());
    }

    public List<SettlementResponseDTO> getAllSettlements() {
        log.debug("Fetching all settlements");
        return settlementRepo.findAll().stream().map(this::toSettlementResponse).collect(Collectors.toList());
    }

    public List<PTPResponseDTO> getPTPs(Long loanId) {
        return ptpRepo.findByLoanAccountId(loanId).stream().map(this::toPTPResponse).collect(Collectors.toList());
    }


    private void sendNotification(Long customerId, Long loanAccountId, String message) {
        try {
            Map<String, Object> notifyBody = new HashMap<>();
            notifyBody.put("customerId", customerId != null ? customerId : 0L);
            notifyBody.put("loanAccountId", loanAccountId);
            notifyBody.put("channel", "INAPP");
            notifyBody.put("notificationType", "PTP");
            notifyBody.put("message", message);
            notificationClient.send(notifyBody);
        } catch (Exception e) {
            log.warn("Notification failed for loan={} — {}", loanAccountId, e.getMessage());
        }
    }



    private PTPResponseDTO toPTPResponse(PTP p) {
        return PTPResponseDTO.builder()
                .ptpId(p.getPtpId())
                .loanAccountId(p.getLoanAccountId())
                .agentId(p.getAgentId())
                .customerId(p.getCustomerId())
                .promisedAmount(p.getPromisedAmount())
                .promisedDate(p.getPromisedDate() != null ? p.getPromisedDate().toString() : null)
                .channel(p.getChannel())
                .promisedBy(p.getPromisedBy())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .build();
    }

    private PaymentResponseDTO toPaymentResponse(PaymentRef p) {
        return PaymentResponseDTO.builder()// create respone obj
                .paymentId(p.getPaymentId())
                .loanAccountId(p.getLoanAccountId())
                .agentId(p.getAgentId())
                .customerId(p.getCustomerId())
                .amount(p.getAmount())
                .paymentDate(p.getPaymentDate() != null ? p.getPaymentDate().toString() : null)
                .paymentMode(p.getPaymentMode())
                .referenceNumber(p.getReferenceNumber())
                .status(p.getStatus() != null ? p.getStatus().name() : null)
                .build();
    }

    private SettlementResponseDTO toSettlementResponse(Settlement s) {
        return SettlementResponseDTO.builder()
                .settlementId(s.getSettlementId())
                .loanAccountId(s.getLoanAccountId())
                .agentId(s.getAgentId())
                .customerId(s.getCustomerId())
                .settlementAmount(s.getSettlementAmount())
                .waiverAmount(s.getWaiverAmount())
                .reason(s.getReason())
                .approvalStatus(s.getApprovalStatus() != null ? s.getApprovalStatus().name() : null)
                .status(s.getStatus() != null ? s.getStatus().name() : null)
                .build();
    }
}
