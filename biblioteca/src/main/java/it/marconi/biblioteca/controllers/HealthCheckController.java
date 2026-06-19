package it.marconi.biblioteca.controllers;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import it.marconi.biblioteca.domain.APIResponse;

/**
 * Test controller for monitoring demonstration
 * Provides endpoints to simulate errors for testing alert rules
 */
@RestController
@RequestMapping("/test")
@Tag(name = "Testing", description = "Endpoints for testing and monitoring verification")
public class HealthCheckController {

    @GetMapping("/health")
    @Operation(summary = "Simple health check endpoint")
    public APIResponse<String> health() {
        return APIResponse.ok("OK", "Application is running");
    }

    @GetMapping("/error")
    @Operation(summary = "Generates a single HTTP 500 error for testing alerts")
    public APIResponse<String> generateError() {
        throw new RuntimeException("Intentional error for monitoring test");
    }

    @GetMapping("/stress")
    @Operation(summary = "Generates 20 HTTP 500 errors in rapid succession for testing alert thresholds")
    public APIResponse<String> generateStress() {
        for (int i = 0; i < 20; i++) {
            // Creiamo una copia "immutabile" di i per la lambda expression
            final int errorNumber = i; 
            
            new Thread(() -> {
                try {
                    Thread.sleep(100);
                    throw new RuntimeException("Stress test error " + errorNumber);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }).start();
        }

        throw new RuntimeException("Stress test initiated - generating 20 errors");
    }
}