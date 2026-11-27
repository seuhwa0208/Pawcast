package com.project.pawcast.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** 상품·리뷰 이미지를 브라우저가 읽을 수 있는 정적 경로로 공개한다. */
@Configuration
public class ProductUploadResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String productLocation = Path.of("uploads/products").toAbsolutePath().normalize().toUri().toString();
        String reviewLocation = Path.of("uploads/images").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/products/**").addResourceLocations(productLocation);
        registry.addResourceHandler("/uploads/images/**").addResourceLocations(reviewLocation);
    }
}
