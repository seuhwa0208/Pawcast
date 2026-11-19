package com.project.pawcast;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
// JWT 등 타입 기반 설정을 Spring Bean으로 등록한다.
@ConfigurationPropertiesScan
public class PawcastApplication {

	public static void main(String[] args) {
        SpringApplication.run(PawcastApplication.class, args);
	}

}
