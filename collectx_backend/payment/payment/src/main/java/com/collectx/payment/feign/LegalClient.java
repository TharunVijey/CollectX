package com.collectx.payment.feign;

import com.collectx.payment.feign.fallback.LegalClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "legal-service", fallback = LegalClientFallback.class)
public interface LegalClient {

    @PostMapping("/legal/on-payment")
    void onPayment(@RequestBody Map<String, Object> request);
}
