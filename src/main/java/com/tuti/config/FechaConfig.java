package com.tuti.config;

import java.time.Clock;
import java.time.ZoneId;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FechaConfig {
    @Bean
    public Clock clock(@Value("${hogarya.zona-horaria}") String zona) {
        return Clock.system(ZoneId.of(zona));
    }
}

