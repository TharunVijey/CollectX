package com.collectx.payment.controller;

import com.collectx.payment.dto.*;
import com.collectx.payment.exception.GlobalExceptionHandler;
import com.collectx.payment.service.PaymentService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("PaymentController MockMvc Tests")
class PaymentControllerTest {

    @Mock           PaymentService  service;
    @InjectMocks    PaymentController controller;

    private MockMvc      mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setup() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        mockMvc = MockMvcBuilders
                .standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }



    @Test
    @DisplayName("POST /payment/ptp — should create PTP and return 200")
    void createPTP_returns200WithPTPResponse() throws Exception {
        PTPRequestDTO req = new PTPRequestDTO();
        req.setLoanAccountId(1L);
        req.setAgentId(2L);
        req.setCustomerId(3L);
        req.setPromisedAmount(5000.0);
        req.setPromisedDate("2026-06-01");
        req.setChannel("PHONE");
        req.setPromisedBy("Customer");

        PTPResponseDTO response = PTPResponseDTO.builder()
                .ptpId(10L)
                .loanAccountId(1L)
                .agentId(2L)
                .customerId(3L)
                .promisedAmount(5000.0)
                .promisedDate("2026-06-01")
                .channel("PHONE")
                .promisedBy("Customer")
                .status("OPEN")
                .build();

        when(service.createPTP(any(PTPRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/payment/ptp")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ptpId").value(10))
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.promisedAmount").value(5000.0))
                .andExpect(jsonPath("$.channel").value("PHONE"));
    }



    @Test
    @DisplayName("POST /payment/create — should record payment and return 200")
    void makePayment_returns200WithPaymentResponse() throws Exception {
        PaymentRequestDTO req = new PaymentRequestDTO();
        req.setLoanAccountId(1L);
        req.setAgentId(2L);
        req.setCustomerId(3L);
        req.setAmount(5000.0);
        req.setPaymentMode("CASH");
        req.setReferenceNumber("REF001");

        PaymentResponseDTO response = PaymentResponseDTO.builder()
                .paymentId(100L)
                .loanAccountId(1L)
                .agentId(2L)
                .customerId(3L)
                .amount(5000.0)
                .paymentDate("2026-05-18")
                .paymentMode("CASH")
                .referenceNumber("REF001")
                .status("POSTED")
                .build();

        when(service.makePayment(any(PaymentRequestDTO.class), anyString())).thenReturn(response);

        mockMvc.perform(post("/payment/create")
                        .header("Authorization", "Bearer test-token")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.paymentId").value(100))
                .andExpect(jsonPath("$.status").value("POSTED"))
                .andExpect(jsonPath("$.amount").value(5000.0))
                .andExpect(jsonPath("$.paymentMode").value("CASH"));
    }



    @Test
    @DisplayName("POST /payment/settlement — should create settlement request and return 200")
    void requestSettlement_returns200WithSettlementResponse() throws Exception {
        SettlementRequestDTO req = new SettlementRequestDTO();
        req.setLoanAccountId(1L);
        req.setAgentId(2L);
        req.setCustomerId(3L);
        req.setSettlementAmount(80000.0);
        req.setWaiverAmount(20000.0);
        req.setReason("Financial hardship");

        SettlementResponseDTO response = SettlementResponseDTO.builder()
                .settlementId(50L)
                .loanAccountId(1L)
                .settlementAmount(80000.0)
                .waiverAmount(20000.0)
                .reason("Financial hardship")
                .approvalStatus("REQUESTED")
                .status("ACTIVE")
                .build();

        when(service.requestSettlement(any(SettlementRequestDTO.class))).thenReturn(response);

        mockMvc.perform(post("/payment/settlement")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.settlementId").value(50))
                .andExpect(jsonPath("$.approvalStatus").value("REQUESTED"))
                .andExpect(jsonPath("$.status").value("ACTIVE"))
                .andExpect(jsonPath("$.waiverAmount").value(20000.0));
    }



    @Test
    @DisplayName("GET /payment/ptp/all — should return list of all PTPs")
    void getAllPTPs_returns200WithList() throws Exception {
        PTPResponseDTO ptp = PTPResponseDTO.builder()
                .ptpId(1L)
                .loanAccountId(10L)
                .promisedAmount(5000.0)
                .status("OPEN")
                .build();

        when(service.getAllPTPs()).thenReturn(List.of(ptp));

        mockMvc.perform(get("/payment/ptp/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].ptpId").value(1))
                .andExpect(jsonPath("$[0].status").value("OPEN"));
    }

    @Test
    @DisplayName("GET /payment/ptp/all — empty list should return empty JSON array")
    void getAllPTPs_emptyList_returnsEmptyArray() throws Exception {
        when(service.getAllPTPs()).thenReturn(List.of());

        mockMvc.perform(get("/payment/ptp/all"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(0));
    }



    @Test
    @DisplayName("GET /payment/payments — should return list of all payments")
    void getAllPayments_returns200WithList() throws Exception {
        PaymentResponseDTO payment = PaymentResponseDTO.builder()
                .paymentId(100L)
                .loanAccountId(1L)
                .amount(5000.0)
                .paymentMode("CASH")
                .status("POSTED")
                .build();

        when(service.getAllPayments()).thenReturn(List.of(payment));

        mockMvc.perform(get("/payment/payments"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].paymentId").value(100))
                .andExpect(jsonPath("$[0].status").value("POSTED"));
    }



    @Test
    @DisplayName("GET /payment/settlements — should return list of all settlements")
    void getAllSettlements_returns200WithList() throws Exception {
        SettlementResponseDTO settlement = SettlementResponseDTO.builder()
                .settlementId(50L)
                .loanAccountId(1L)
                .settlementAmount(80000.0)
                .approvalStatus("REQUESTED")
                .status("ACTIVE")
                .build();

        when(service.getAllSettlements()).thenReturn(List.of(settlement));

        mockMvc.perform(get("/payment/settlements"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].settlementId").value(50))
                .andExpect(jsonPath("$[0].approvalStatus").value("REQUESTED"));
    }



    @Test
    @DisplayName("GET /payment/ptp/{loanId} — should return PTPs for given loan")
    void getPTPByLoanId_returns200WithList() throws Exception {
        PTPResponseDTO ptp = PTPResponseDTO.builder()
                .ptpId(5L)
                .loanAccountId(10L)
                .promisedAmount(3000.0)
                .status("OPEN")
                .build();

        when(service.getPTPs(10L)).thenReturn(List.of(ptp));

        mockMvc.perform(get("/payment/ptp/10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].loanAccountId").value(10));
    }

    @Test
    @DisplayName("GET /payment/ptp/loan/{loanId} — should return PTPs for given loan")
    void getPTPByLoanPath_returns200WithList() throws Exception {
        PTPResponseDTO ptp = PTPResponseDTO.builder()
                .ptpId(6L)
                .loanAccountId(20L)
                .promisedAmount(4000.0)
                .status("KEPT")
                .build();

        when(service.getPTPs(20L)).thenReturn(List.of(ptp));

        mockMvc.perform(get("/payment/ptp/loan/20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].ptpId").value(6))
                .andExpect(jsonPath("$[0].status").value("KEPT"));
    }



    @Test
    @DisplayName("PATCH /settlement/{id}/approve?decision=APPROVED — should return HONORED")
    void approveSettlement_approved_returnsHONORED() throws Exception {
        SettlementResponseDTO response = SettlementResponseDTO.builder()
                .settlementId(50L)
                .approvalStatus("APPROVED")
                .status("HONORED")
                .build();

        when(service.approveSettlement(50L, "APPROVED")).thenReturn(response);

        mockMvc.perform(patch("/payment/settlement/50/approve")
                        .param("decision", "APPROVED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("APPROVED"))
                .andExpect(jsonPath("$.status").value("HONORED"));
    }

    @Test
    @DisplayName("PATCH /settlement/{id}/approve?decision=REJECTED — should return DEFAULTED")
    void approveSettlement_rejected_returnsDEFAULTED() throws Exception {
        SettlementResponseDTO response = SettlementResponseDTO.builder()
                .settlementId(51L)
                .approvalStatus("REJECTED")
                .status("DEFAULTED")
                .build();

        when(service.approveSettlement(51L, "REJECTED")).thenReturn(response);

        mockMvc.perform(patch("/payment/settlement/51/approve")
                        .param("decision", "REJECTED"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approvalStatus").value("REJECTED"))
                .andExpect(jsonPath("$.status").value("DEFAULTED"));
    }

    @Test
    @DisplayName("PATCH /settlement/{id}/approve — settlement not found should return 400")
    void approveSettlement_notFound_returns400() throws Exception {
        when(service.approveSettlement(999L, "APPROVED"))
                .thenThrow(new RuntimeException("Settlement not found: 999"));

        mockMvc.perform(patch("/payment/settlement/999/approve")
                        .param("decision", "APPROVED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Settlement not found: 999"));
    }

    @Test
    @DisplayName("PATCH /settlement/{id}/approve — invalid decision should return 400")
    void approveSettlement_invalidDecision_returns400() throws Exception {
        when(service.approveSettlement(50L, "MAYBE"))
                .thenThrow(new RuntimeException("Invalid decision: MAYBE. Use APPROVED or REJECTED"));

        mockMvc.perform(patch("/payment/settlement/50/approve")
                        .param("decision", "MAYBE"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid decision: MAYBE. Use APPROVED or REJECTED"));
    }
}
