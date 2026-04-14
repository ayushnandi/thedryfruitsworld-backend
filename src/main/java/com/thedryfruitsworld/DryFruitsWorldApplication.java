package com.thedryfruitsworld;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;

@SpringBootApplication
@EnableCaching
public class DryFruitsWorldApplication {
    public static void main(String[] args) {
        SpringApplication.run(DryFruitsWorldApplication.class, args);
    }
}
