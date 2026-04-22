package com.hexaweb.backendcluverse.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig {

    @Value("${app.sponsor-files-dir}")
    private String sponsorFilesDir;

    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**") 
                        .allowedOrigins("http://localhost:4200", "http://localhost:3000")
                        .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS");
            }

            @Override
            public void addResourceHandlers(ResourceHandlerRegistry registry) {
                String normalizedPath = sponsorFilesDir.replace("\\", "/");
                if (!normalizedPath.endsWith("/")) {
                    normalizedPath += "/";
                }

                registry.addResourceHandler("/assets/sponsorfiles/**")
                        .addResourceLocations("file:" + normalizedPath);
            }
        };
    }
}