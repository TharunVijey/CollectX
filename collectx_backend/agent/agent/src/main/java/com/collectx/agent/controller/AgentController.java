package com.collectx.agent.controller;

import com.collectx.agent.dto.*;
import com.collectx.agent.service.AgentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/agent")
@RequiredArgsConstructor
public class AgentController {

    private final AgentService service;

    // ── PTP ───────────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'SUPERVISOR')")
    @PostMapping("/ptp")
    public String createPTP(@RequestBody AgentPTPRequestDTO dto,
                            @RequestHeader("Authorization") String token) {
        return service.createPTPFromAgent(dto, token);
    }

    // ── CASE ──────────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'SUPERVISOR')")
    @PostMapping("/case")
    public CaseResponseDTO createCase(@RequestBody CaseRequestDTO dto) {
        return service.createCase(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'SUPERVISOR', 'COMPLIANCE')")
    @GetMapping("/cases/{loanId}")
    public List<CaseResponseDTO> getCases(@PathVariable Long loanId) {
        return service.getCases(loanId);
    }

    // ── NOTE ──────────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'SUPERVISOR')")
    @PostMapping("/note")
    public NoteResponseDTO addNote(@RequestBody NoteRequestDTO dto) {
        return service.addNote(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'SUPERVISOR')")
    @GetMapping("/notes/loan/{loanId}")
    public List<NoteResponseDTO> getNotesByLoan(@PathVariable Long loanId) {
        return service.getNotesByLoan(loanId);
    }

    // ── TASK ──────────────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'SUPERVISOR')")
    @PostMapping("/task")
    public TaskResponseDTO createTask(@RequestBody TaskRequestDTO dto) {
        return service.createTask(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'SUPERVISOR')")
    @GetMapping("/tasks/agent/{agentId}")
    public List<TaskResponseDTO> getTasksByAgent(@PathVariable Long agentId) {
        return service.getTasksByAgent(agentId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'SUPERVISOR')")
    @PatchMapping("/task/{taskId}/status")
    public TaskResponseDTO updateTaskStatus(@PathVariable Long taskId, @RequestParam String status) {
        return service.updateTaskStatus(taskId, status);
    }

    // ── HARDSHIP FLAG ──────────────────────────────────────────────────────────

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'SUPERVISOR', 'COMPLIANCE')")
    @PostMapping("/hardship")
    public HardshipResponseDTO createHardship(@RequestBody HardshipRequestDTO dto) {
        return service.createHardship(dto);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'AGENT', 'SUPERVISOR', 'COMPLIANCE')")
    @GetMapping("/hardship/loan/{loanId}")
    public List<HardshipResponseDTO> getHardshipsByLoan(@PathVariable Long loanId) {
        return service.getHardshipsByLoan(loanId);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SUPERVISOR', 'COMPLIANCE')")
    @GetMapping("/hardships")
    public List<HardshipResponseDTO> getAllHardships() {
        return service.getAllHardships();
    }
}
