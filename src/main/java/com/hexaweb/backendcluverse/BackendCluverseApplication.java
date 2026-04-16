package com.hexaweb.backendcluverse;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class BackendCluverseApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendCluverseApplication.class, args);
    }

}
