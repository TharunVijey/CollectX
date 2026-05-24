package com.collectx.dunning.controller;

import com.collectx.dunning.dto.ConsentRequestDTO;
import com.collectx.dunning.dto.ConsentResponseDTO;
import com.collectx.dunning.service.ConsentService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dunning/consents")
@RequiredArgsConstructor
public class ConsentsController {

    private final ConsentService consentService;


    @PreAuthorize("hasAnyRole('ADMIN','SUPERVISOR')")
    @PutMapping
    public ConsentResponseDTO upsert(@RequestBody ConsentRequestDTO req) {
        return consentService.upsert(req);
    }

    @PreAuthorize("hasAnyRole('ADMIN','COMPLIANCE','SUPERVISOR')")
    @GetMapping("/{customerId}")
    public List<ConsentResponseDTO> list(@PathVariable Long customerId) {
        return consentService.listByCustomer(customerId);
    }
}
