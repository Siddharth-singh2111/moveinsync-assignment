package com.moveinsync.distance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the GPS gap backfill and billable distance reconstruction service.
 */
@SpringBootApplication
public class DistanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DistanceApplication.class, args);
    }
}
