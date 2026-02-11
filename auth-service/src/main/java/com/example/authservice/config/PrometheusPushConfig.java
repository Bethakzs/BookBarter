package com.example.authservice.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import io.prometheus.client.exporter.PushGateway;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class PrometheusPushConfig implements ApplicationRunner {

    private final MeterRegistry meterRegistry;
    private final String baseUrl;
    private final String job;
    private PushGateway pushGateway;
    private ScheduledExecutorService scheduler;

    public PrometheusPushConfig(
            MeterRegistry meterRegistry,
            @Value("${management.metrics.export.prometheus.pushgateway.base-url:http://pushgateway:9091}") String baseUrl,
            @Value("${management.metrics.export.prometheus.pushgateway.job:auth-service}") String job) {
        
        log.info("🔧 Constructor called! MeterRegistry type: {}", meterRegistry.getClass().getName());
        this.meterRegistry = meterRegistry;
        this.baseUrl = baseUrl;
        this.job = job;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("🚀 Initializing Prometheus Pushgateway...");
        
        if (!(meterRegistry instanceof PrometheusMeterRegistry)) {
            log.error("❌ MeterRegistry is not PrometheusMeterRegistry: {}", meterRegistry.getClass());
            return;
        }
        
        try {
            this.pushGateway = new PushGateway(new URL(baseUrl));
            log.info("✅ Prometheus Pushgateway configured: {} (job: {})", baseUrl, job);
            
            // Start scheduled pushing
            scheduler = Executors.newSingleThreadScheduledExecutor();
            scheduler.scheduleAtFixedRate(this::pushMetrics, 10, 10, TimeUnit.SECONDS);
            log.info("📊 Started pushing metrics every 10 seconds");
            
        } catch (Exception e) {
            log.error("❌ Failed to configure Pushgateway", e);
        }
    }

    private void pushMetrics() {
        try {
            PrometheusMeterRegistry prometheusRegistry = (PrometheusMeterRegistry) meterRegistry;
            pushGateway.pushAdd(prometheusRegistry.getPrometheusRegistry(), job);
            log.info("✅ Pushed metrics to Pushgateway (job: {})", job);
        } catch (Exception e) {
            log.error("❌ Failed to push metrics to Pushgateway", e);
        }
    }

    @PreDestroy
    public void shutdown() {
        if (scheduler != null) {
            scheduler.shutdown();
        }
        try {
            pushMetrics();
            log.info("✅ Final metrics pushed to Pushgateway");
        } catch (Exception e) {
            log.error("❌ Failed to push final metrics", e);
        }
    }
}

