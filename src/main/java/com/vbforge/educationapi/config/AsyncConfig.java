package com.vbforge.educationapi.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableAsync          // enables @Async on service methods
@EnableScheduling     // enables @Scheduled — useful for due date reminder jobs later
public class AsyncConfig {
}