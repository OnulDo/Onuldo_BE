package com.example.onuldo;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class OnuldoApplication {

	public static void main(String[] args) {
		SpringApplication.run(OnuldoApplication.class, args);
	}

}
