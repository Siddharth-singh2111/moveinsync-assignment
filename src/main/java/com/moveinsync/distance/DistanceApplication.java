package com.moveinsync.distance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Entry point for the GPS gap backfill and billable distance reconstruction service.
 */
@SpringBootApplication
@EnableScheduling
public class DistanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistanceApplication.class, args);
    }
}
