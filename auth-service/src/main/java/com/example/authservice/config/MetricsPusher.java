package com.example.authservice.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MetricsPusher {

    @PostConstruct
    public void init() {
        log.info("========================================");
        log.info("🎉 MetricsPusher Component CREATED!");
        log.info("========================================");
    }
}



