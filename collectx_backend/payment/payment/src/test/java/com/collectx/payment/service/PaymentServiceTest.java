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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentService Unit Tests")
class PaymentServiceTest {

    @Mock PTPRepository        ptpRepo;
    @Mock PaymentRepository    paymentRepo;
    @Mock SettlementRepository settlementRepo;
    @Mock NotificationClient   notificationClient;
    @Mock ReportingClient      reportingClient;
    @Mock PortfolioClient      portfolioClient;
    @Mock LegalClient          legalClient;

    @InjectMocks PaymentService service;


    @Test
    @DisplayName("createPTP: should save PTP and return OPEN status")
    void createPTP_success_returnsPTPWithOpenStatus() {
        PTPRequestDTO dto = new PTPRequestDTO();
        dto.setLoanAccountId(1L);
        dto.setAgentId(2L);
        dto.setCustomerId(3L);
        dto.setPromisedAmount(5000.0);
        dto.setPromisedDate("2026-06-01");
        dto.setChannel("PHONE");
        dto.setPromisedBy("Customer");

        PTP saved = new PTP();
        saved.setPtpId(10L);
        saved.setLoanAccountId(1L);
        saved.setAgentId(2L);
        saved.setCustomerId(3L);
        saved.setPromisedAmount(5000.0);
        saved.setPromisedDate(LocalDate.of(2026, 6, 1));
        saved.setChannel("PHONE");
        saved.setPromisedBy("Customer");
        saved.setStatus(PTPStatus.OPEN);

        when(ptpRepo.save(any(PTP.class))).thenReturn(saved);

        PTPResponseDTO result = service.createPTP(dto);

        assertThat(result.getPtpId()).isEqualTo(10L);
        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getPromisedAmount()).isEqualTo(5000.0);
        assertThat(result.getChannel()).isEqualTo("PHONE");
        verify(ptpRepo, times(1)).save(any(PTP.class));
    }

    @Test
    @DisplayName("createPTP: should handle null promised date gracefully")
    void createPTP_withNullPromisedDate_savesSuccessfully() {
        PTPRequestDTO dto = new PTPRequestDTO();
        dto.setLoanAccountId(1L);
        dto.setPromisedAmount(3000.0);
        dto.setPromisedDate(null);

        PTP saved = new PTP();
        saved.setPtpId(11L);
        saved.setStatus(PTPStatus.OPEN);

        when(ptpRepo.save(any(PTP.class))).thenReturn(saved);

        PTPResponseDTO result = service.createPTP(dto);

        assertThat(result.getPtpId()).isEqualTo(11L);
        assertThat(result.getStatus()).isEqualTo("OPEN");
        assertThat(result.getPromisedDate()).isNull();
    }



    @Test
    @DisplayName("makePayment: payment >= promised amount should mark PTP as KEPT")
    void makePayment_amountMeetsPTP_marksKEPT() {
        PaymentRequestDTO dto = buildPaymentRequest(1L, 5000.0);

        PaymentRef savedPayment = buildSavedPayment(100L, 5000.0);

        PTP openPtp = new PTP();
        openPtp.setPtpId(20L);
        openPtp.setStatus(PTPStatus.OPEN);
        openPtp.setPromisedAmount(5000.0);

        when(paymentRepo.save(any(PaymentRef.class))).thenReturn(savedPayment);
        when(ptpRepo.findByLoanAccountId(1L)).thenReturn(List.of(openPtp));
        when(ptpRepo.save(any(PTP.class))).thenReturn(openPtp);

        PaymentResponseDTO result = service.makePayment(dto, "Bearer token");

        assertThat(result.getPaymentId()).isEqualTo(100L);
        assertThat(result.getStatus()).isEqualTo("POSTED");
        assertThat(openPtp.getStatus()).isEqualTo(PTPStatus.KEPT);
        verify(ptpRepo, times(1)).save(openPtp);
    }

    @Test
    @DisplayName("makePayment: payment < promised amount should mark PTP as BROKEN")
    void makePayment_amountBelowPTP_marksBROKEN() {
        PaymentRequestDTO dto = buildPaymentRequest(1L, 2000.0); // less than promised

        PaymentRef savedPayment = buildSavedPayment(101L, 2000.0);

        PTP openPtp = new PTP();
        openPtp.setPtpId(21L);
        openPtp.setStatus(PTPStatus.OPEN);
        openPtp.setPromisedAmount(5000.0); // payment(2000) < promised(5000) → BROKEN

        when(paymentRepo.save(any(PaymentRef.class))).thenReturn(savedPayment);
        when(ptpRepo.findByLoanAccountId(1L)).thenReturn(List.of(openPtp));
        when(ptpRepo.save(any(PTP.class))).thenReturn(openPtp);

        service.makePayment(dto, "Bearer token");

        assertThat(openPtp.getStatus()).isEqualTo(PTPStatus.BROKEN);
        verify(ptpRepo, times(1)).save(openPtp);
    }

    @Test
    @DisplayName("makePayment: already KEPT or BROKEN PTPs should not be updated again")
    void makePayment_nonOpenPTPs_notUpdated() {
        PaymentRequestDTO dto = buildPaymentRequest(1L, 5000.0);
        PaymentRef savedPayment = buildSavedPayment(102L, 5000.0);

        PTP keptPtp = new PTP();
        keptPtp.setPtpId(22L);
        keptPtp.setStatus(PTPStatus.KEPT);

        when(paymentRepo.save(any(PaymentRef.class))).thenReturn(savedPayment);
        when(ptpRepo.findByLoanAccountId(1L)).thenReturn(List.of(keptPtp));

        service.makePayment(dto, "Bearer token");

        verify(ptpRepo, never()).save(any(PTP.class));
        assertThat(keptPtp.getStatus()).isEqualTo(PTPStatus.KEPT);
    }

    @Test
    @DisplayName("makePayment: no open PTPs — should only save payment")
    void makePayment_noOpenPTPs_savesPaymentOnly() {
        PaymentRequestDTO dto = buildPaymentRequest(5L, 1000.0);
        PaymentRef savedPayment = buildSavedPayment(103L, 1000.0);

        when(paymentRepo.save(any(PaymentRef.class))).thenReturn(savedPayment);
        when(ptpRepo.findByLoanAccountId(5L)).thenReturn(List.of());

        PaymentResponseDTO result = service.makePayment(dto, "Bearer token");

        assertThat(result.getPaymentId()).isEqualTo(103L);
        verify(ptpRepo, never()).save(any(PTP.class));
    }

    @Test
    @DisplayName("makePayment: portfolio service failure should not stop payment from being saved")
    void makePayment_portfolioServiceFails_stillReturnsPayment() {
        PaymentRequestDTO dto = buildPaymentRequest(1L, 1000.0);
        PaymentRef savedPayment = buildSavedPayment(104L, 1000.0);

        when(paymentRepo.save(any(PaymentRef.class))).thenReturn(savedPayment);
        when(ptpRepo.findByLoanAccountId(1L)).thenReturn(List.of());
        doThrow(new RuntimeException("Portfolio service down"))
                .when(portfolioClient).applyPayment(any());

        PaymentResponseDTO result = service.makePayment(dto, "Bearer token");

        // payment still succeeds even when portfolio Feign fails
        assertThat(result.getPaymentId()).isEqualTo(104L);
        assertThat(result.getStatus()).isEqualTo("POSTED");
    }

    @Test
    @DisplayName("makePayment: reporting service failure should not stop payment from being saved")
    void makePayment_reportingServiceFails_stillReturnsPayment() {
        PaymentRequestDTO dto = buildPaymentRequest(1L, 1000.0);
        PaymentRef savedPayment = buildSavedPayment(105L, 1000.0);

        when(paymentRepo.save(any(PaymentRef.class))).thenReturn(savedPayment);
        when(ptpRepo.findByLoanAccountId(1L)).thenReturn(List.of());
        doThrow(new RuntimeException("Reporting service down"))
                .when(reportingClient).sendPerformance(any());

        PaymentResponseDTO result = service.makePayment(dto, "Bearer token");

        assertThat(result.getPaymentId()).isEqualTo(105L);
    }



    @Test
    @DisplayName("requestSettlement: should save with REQUESTED approval and ACTIVE status")
    void requestSettlement_success_returnsRequestedAndActiveStatus() {
        SettlementRequestDTO dto = new SettlementRequestDTO();
        dto.setLoanAccountId(1L);
        dto.setAgentId(2L);
        dto.setCustomerId(3L);
        dto.setSettlementAmount(80000.0);
        dto.setWaiverAmount(20000.0);
        dto.setReason("Financial hardship");

        Settlement saved = new Settlement();
        saved.setSettlementId(50L);
        saved.setLoanAccountId(1L);
        saved.setSettlementAmount(80000.0);
        saved.setWaiverAmount(20000.0);
        saved.setReason("Financial hardship");
        saved.setApprovalStatus(ApprovalStatus.REQUESTED);
        saved.setStatus(SettlementStatus.ACTIVE);

        when(settlementRepo.save(any(Settlement.class))).thenReturn(saved);

        SettlementResponseDTO result = service.requestSettlement(dto);

        assertThat(result.getSettlementId()).isEqualTo(50L);
        assertThat(result.getApprovalStatus()).isEqualTo("REQUESTED");
        assertThat(result.getStatus()).isEqualTo("ACTIVE");
        assertThat(result.getSettlementAmount()).isEqualTo(80000.0);
        verify(settlementRepo, times(1)).save(any(Settlement.class));
    }



    @Test
    @DisplayName("approveSettlement: APPROVED decision should set status to HONORED")
    void approveSettlement_approved_setsStatusToHONORED() {
        Settlement s = buildRequestedSettlement(50L);

        when(settlementRepo.findById(50L)).thenReturn(Optional.of(s));
        when(settlementRepo.save(any())).thenReturn(s);

        SettlementResponseDTO result = service.approveSettlement(50L, "APPROVED");

        assertThat(result.getApprovalStatus()).isEqualTo("APPROVED");
        assertThat(result.getStatus()).isEqualTo("HONORED");
    }

    @Test
    @DisplayName("approveSettlement: REJECTED decision should set status to DEFAULTED")
    void approveSettlement_rejected_setsStatusToDEFAULTED() {
        Settlement s = buildRequestedSettlement(51L);

        when(settlementRepo.findById(51L)).thenReturn(Optional.of(s));
        when(settlementRepo.save(any())).thenReturn(s);

        SettlementResponseDTO result = service.approveSettlement(51L, "REJECTED");

        assertThat(result.getApprovalStatus()).isEqualTo("REJECTED");
        assertThat(result.getStatus()).isEqualTo("DEFAULTED");
    }

    @Test
    @DisplayName("approveSettlement: settlement not found should throw RuntimeException")
    void approveSettlement_notFound_throwsRuntimeException() {
        when(settlementRepo.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.approveSettlement(999L, "APPROVED"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Settlement not found");
    }

    @Test
    @DisplayName("approveSettlement: already approved settlement should throw RuntimeException")
    void approveSettlement_alreadyApproved_throwsRuntimeException() {
        Settlement s = new Settlement();
        s.setSettlementId(52L);
        s.setApprovalStatus(ApprovalStatus.APPROVED); // already processed

        when(settlementRepo.findById(52L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.approveSettlement(52L, "APPROVED"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already been approved");
    }

    @Test
    @DisplayName("approveSettlement: already rejected settlement should throw RuntimeException")
    void approveSettlement_alreadyRejected_throwsRuntimeException() {
        Settlement s = new Settlement();
        s.setSettlementId(53L);
        s.setApprovalStatus(ApprovalStatus.REJECTED); // already rejected

        when(settlementRepo.findById(53L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.approveSettlement(53L, "REJECTED"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already been rejected");
    }

    @Test
    @DisplayName("approveSettlement: invalid decision value should throw RuntimeException")
    void approveSettlement_invalidDecision_throwsRuntimeException() {
        Settlement s = buildRequestedSettlement(54L);

        when(settlementRepo.findById(54L)).thenReturn(Optional.of(s));

        assertThatThrownBy(() -> service.approveSettlement(54L, "MAYBE"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid decision");
    }



    @Test
    @DisplayName("getAllPTPs: should return mapped list of PTPResponseDTOs")
    void getAllPTPs_returnsListOfResponses() {
        PTP ptp = new PTP();
        ptp.setPtpId(1L);
        ptp.setLoanAccountId(10L);
        ptp.setStatus(PTPStatus.OPEN);

        when(ptpRepo.findAll()).thenReturn(List.of(ptp));

        List<PTPResponseDTO> result = service.getAllPTPs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPtpId()).isEqualTo(1L);
        assertThat(result.get(0).getStatus()).isEqualTo("OPEN");
    }

    @Test
    @DisplayName("getAllPTPs: empty repository should return empty list")
    void getAllPTPs_emptyRepo_returnsEmptyList() {
        when(ptpRepo.findAll()).thenReturn(List.of());

        List<PTPResponseDTO> result = service.getAllPTPs();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("getAllPayments: should return mapped list of PaymentResponseDTOs")
    void getAllPayments_returnsListOfResponses() {
        PaymentRef payment = new PaymentRef();
        payment.setPaymentId(1L);
        payment.setAmount(5000.0);
        payment.setPaymentDate(LocalDate.now());
        payment.setStatus(PaymentStatus.POSTED);

        when(paymentRepo.findAll()).thenReturn(List.of(payment));

        List<PaymentResponseDTO> result = service.getAllPayments();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getPaymentId()).isEqualTo(1L);
        assertThat(result.get(0).getStatus()).isEqualTo("POSTED");
    }

    @Test
    @DisplayName("getAllSettlements: should return mapped list of SettlementResponseDTOs")
    void getAllSettlements_returnsListOfResponses() {
        Settlement s = new Settlement();
        s.setSettlementId(1L);
        s.setSettlementAmount(50000.0);
        s.setApprovalStatus(ApprovalStatus.REQUESTED);
        s.setStatus(SettlementStatus.ACTIVE);

        when(settlementRepo.findAll()).thenReturn(List.of(s));

        List<SettlementResponseDTO> result = service.getAllSettlements();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getSettlementId()).isEqualTo(1L);
        assertThat(result.get(0).getApprovalStatus()).isEqualTo("REQUESTED");
    }

    @Test
    @DisplayName("getPTPs: should return PTPs filtered by loanAccountId")
    void getPTPs_byLoanId_returnsFilteredPTPs() {
        PTP ptp = new PTP();
        ptp.setPtpId(5L);
        ptp.setLoanAccountId(10L);
        ptp.setStatus(PTPStatus.OPEN);

        when(ptpRepo.findByLoanAccountId(10L)).thenReturn(List.of(ptp));

        List<PTPResponseDTO> result = service.getPTPs(10L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getLoanAccountId()).isEqualTo(10L);
    }

    @Test
    @DisplayName("getPTPs: loan with no PTPs should return empty list")
    void getPTPs_noPTPs_returnsEmpty() {
        when(ptpRepo.findByLoanAccountId(99L)).thenReturn(List.of());

        List<PTPResponseDTO> result = service.getPTPs(99L);

        assertThat(result).isEmpty();
    }


    private PaymentRequestDTO buildPaymentRequest(Long loanId, Double amount) {
        PaymentRequestDTO dto = new PaymentRequestDTO();
        dto.setLoanAccountId(loanId);
        dto.setAgentId(2L);
        dto.setCustomerId(3L);
        dto.setAmount(amount);
        dto.setPaymentMode("CASH");
        dto.setReferenceNumber("REF-" + loanId);
        return dto;
    }

    private PaymentRef buildSavedPayment(Long paymentId, Double amount) {
        PaymentRef p = new PaymentRef();
        p.setPaymentId(paymentId);
        p.setLoanAccountId(1L);
        p.setAmount(amount);
        p.setPaymentDate(LocalDate.now());
        p.setStatus(PaymentStatus.POSTED);
        return p;
    }

    private Settlement buildRequestedSettlement(Long id) {
        Settlement s = new Settlement();
        s.setSettlementId(id);
        s.setApprovalStatus(ApprovalStatus.REQUESTED);
        s.setStatus(SettlementStatus.ACTIVE);
        return s;
    }
}
