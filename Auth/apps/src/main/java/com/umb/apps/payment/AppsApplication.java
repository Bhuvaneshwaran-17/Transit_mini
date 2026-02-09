package com.umb.apps.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan; // CORRECT IMPORT
import org.springframework.data.jpa.repository.config.EnableJpaRepositories; // CORRECT IMPORT

@SpringBootApplication
@EnableJpaRepositories(basePackages = "com.umb.apps.payment.repository")
@EntityScan(basePackages = "com.umb.apps.payment.entity")
public class AppsApplication {

	public static void main(String[] args) {
		SpringApplication.run(AppsApplication.class, args);
	}
}