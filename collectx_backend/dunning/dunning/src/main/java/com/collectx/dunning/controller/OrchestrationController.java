package com.collectx.dunning.controller;

import com.collectx.dunning.dto.NextSuggestionResponseDTO;
import com.collectx.dunning.service.OrchestrationService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/dunning/orchestration")
@RequiredArgsConstructor
public class OrchestrationController {

    private final OrchestrationService orchestrationService;


    @PreAuthorize("hasAnyRole('AGENT','SUPERVISOR')")
    @GetMapping("/loans/{loanAccountId}/next-suggestion")
    public NextSuggestionResponseDTO suggest(
            @PathVariable Long   loanAccountId,
            @RequestParam String bucket,
            @RequestParam Long   customerId) {

        return orchestrationService.nextSuggestion(loanAccountId, bucket, customerId);
    }
}
