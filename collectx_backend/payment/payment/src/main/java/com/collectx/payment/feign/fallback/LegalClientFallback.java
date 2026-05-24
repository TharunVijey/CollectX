package com.collectx.payment.feign.fallback;

import com.collectx.payment.feign.LegalClient;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class LegalClientFallback implements LegalClient {

    @Override
    public void onPayment(Map<String, Object> request) {
        // silent fail — legal automation skipped if legal-service is unavailable
    }
}
