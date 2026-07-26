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
    public void addResourceHandlers(
            ResourceHandlerRegistry registry
    ) {
        Path storageDirectory = Paths.get(baseStorageDir)
                .toAbsolutePath()
                .normalize();

        Path imagesDirectory = storageDirectory
                .resolve("images")
                .normalize();

        Path invoicesDirectory = storageDirectory
                .resolve("invoices")
                .normalize();

        registry.addResourceHandler("/images/**")
                .addResourceLocations(
                        toDirectoryResourceLocation(
                                imagesDirectory
                        ),
                        "classpath:/static/images/"
                );

        registry.addResourceHandler("/invoices/**")
                .addResourceLocations(
                        toDirectoryResourceLocation(
                                invoicesDirectory
                        )
                );
    }

    private String toDirectoryResourceLocation(
            Path directory
    ) {
        String location = directory
                .toUri()
                .toString();

        return location.endsWith("/")
                ? location
                : location + "/";
    }
}