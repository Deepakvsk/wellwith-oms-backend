package com.wellwithlife.omsbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@SpringBootApplication
public class OmsbackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(OmsbackendApplication.class, args);
	}

}
