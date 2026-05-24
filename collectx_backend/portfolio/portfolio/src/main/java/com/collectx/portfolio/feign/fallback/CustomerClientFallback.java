package com.collectx.portfolio.feign.fallback;

import com.collectx.portfolio.feign.CustomerClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class CustomerClientFallback implements CustomerClient {

    @Override
    public Map<String, Object> getById(Long id) {
        // Customer service is down — allow loan creation to proceed without blocking
        return Map.of("customerId", id, "name", "Unknown", "_fallback", true);
    }
}
