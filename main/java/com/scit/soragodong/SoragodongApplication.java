package com.scit.soragodong;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class SoragodongApplication {

	public static void main(String[] args) {
		SpringApplication.run(SoragodongApplication.class, args);
	}

}
