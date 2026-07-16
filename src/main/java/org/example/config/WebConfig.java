package org.example.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Value("${app.storage.local-dir:./local-storage/}")
    private String baseStorageDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {

        Path imagesDir = Paths.get(baseStorageDir, "images").toAbsolutePath().normalize();
        Path invoicesDir = Paths.get(baseStorageDir, "invoices").toAbsolutePath().normalize();


        registry.addResourceHandler("/images/**")
                .addResourceLocations("file:" + imagesDir.toString() + "/", "classpath:/static/images/");

        registry.addResourceHandler("/invoices/**")
                .addResourceLocations("file:" + invoicesDir.toString() + "/");
    }
}