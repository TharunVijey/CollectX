package com.collectx.payment.feign;

import com.collectx.payment.feign.fallback.PortfolioClientFallback;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.Map;

@FeignClient(name = "portfolio-service", fallback = PortfolioClientFallback.class)
public interface PortfolioClient {

    @PutMapping("/portfolio/loan/payment")
    String applyPayment(@RequestBody Map<String, Object> request);
}
