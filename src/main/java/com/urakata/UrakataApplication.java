package com.urakata;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

//@SpringBootApplication(exclude = { DataSourceAutoConfiguration.class })
@SpringBootApplication
@EnableScheduling
public class UrakataApplication {

	public static void main(String[] args) {
		SpringApplication.run(UrakataApplication.class, args);
	}

}
