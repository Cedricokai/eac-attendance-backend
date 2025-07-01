package com.eacattendance;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.boot.web.servlet.server.ConfigurableServletWebServerFactory;
import org.springframework.context.annotation.Bean;
import java.net.InetAddress;
import java.net.UnknownHostException;

@SpringBootApplication
public class EacAttendanceApplication {

	public static void main(String[] args) {
		SpringApplication.run(EacAttendanceApplication.class, args);
	}

	@Bean
	public WebServerFactoryCustomizer<ConfigurableServletWebServerFactory> webServerFactoryCustomizer() {
		return factory -> {
			try {
				factory.setAddress(InetAddress.getByName("0.0.0.0")); // Accept connections from any IP
			} catch (UnknownHostException e) {
				throw new RuntimeException(e);
			}
		};
	}
}