package com.example.eac_backend_project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class EacBackendProjectApplication {

	public static void main(String[] args) {
		SpringApplication application = new SpringApplication(EacBackendProjectApplication.class);
		// A private local settings file opts this checkout into local defaults.
		// Explicit active profiles still take precedence; deployments without it are unchanged.
		if (Files.isRegularFile(Path.of(".env.local"))) {
			application.setDefaultProperties(Map.of("spring.profiles.default", "local"));
		}
		application.run(args);
	}

}
