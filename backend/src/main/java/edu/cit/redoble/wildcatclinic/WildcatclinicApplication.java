package edu.cit.redoble.wildcatclinic;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "edu.cit.redoble")
@EntityScan(basePackages = "edu.cit.redoble.entity")
@EnableJpaRepositories(basePackages = "edu.cit.redoble.repository")
public class WildcatclinicApplication {

	public static void main(String[] args) {
		SpringApplication.run(WildcatclinicApplication.class, args);
	}

}
