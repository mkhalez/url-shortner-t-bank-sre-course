package com.coworking.space.urlservice.utils;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class UrlMetrics {
    private final MeterRegistry meterRegistry;
    private final Counter createdRandomUrl;
    private final Counter createdCustomUrl;

    public UrlMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;

        this.createdRandomUrl = Counter.builder("url.created")
                .description("Number of created short urls")
                .tag("type", "random").register(meterRegistry);
        this.createdCustomUrl= Counter.builder("url.created")
                .description("Number of created short urls")
                .tag("type", "custom").register(meterRegistry);
    }

    public void randomCreated() {
        createdRandomUrl.increment();
    }

    public void customCreated() {
        createdCustomUrl.increment();
    }

    public void redirect(boolean found) {
        meterRegistry.counter("url.redirects", "result", found ? "found" : "not_found").increment();
    }

    public void customRejected(String reason) {
        meterRegistry.counter("url.custom.rejected", "reason", reason).increment();
    }
}
