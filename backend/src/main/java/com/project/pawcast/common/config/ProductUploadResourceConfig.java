package com.project.pawcast.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;

/** 상품 이미지만 브라우저에 공개한다. 리뷰 업로드 파일은 이 경로에 포함되지 않는다. */
@Configuration
public class ProductUploadResourceConfig implements WebMvcConfigurer {
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String location = Path.of("uploads/products").toAbsolutePath().normalize().toUri().toString();
        registry.addResourceHandler("/uploads/products/**").addResourceLocations(location);
    }
}
