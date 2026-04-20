package com.hexaweb.backendcluverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BackendCluverseApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendCluverseApplication.class, args);
    }

}
