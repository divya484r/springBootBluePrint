package com.sample.springbootsampleapp.server;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import java.util.Timer;
import java.util.TimerTask;

/**
 * {@link HealthIndicator} determines if the service is 'healthy'.
 */
@Component
public class HealthCheck implements HealthIndicator {

    private final Timer timer = new Timer("HealthCheckTaskTimer", true);
    private volatile Health currentHealth = Health.outOfService().build();

    public HealthCheck() {
        timer.schedule(new HealthCheckTask(), 0L, 10000L);
    }

    @Override
    public Health health() {
        return currentHealth;
    }

    /**
     * Task that determines the health of the service.
     */
    private class HealthCheckTask extends TimerTask {

        @Override
        public void run() {
            currentHealth = Health.up().build();
        }
    }
}
