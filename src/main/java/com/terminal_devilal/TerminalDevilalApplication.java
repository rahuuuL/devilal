package com.terminal_devilal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class TerminalDevilalApplication {

	public static void main(String[] args) {
		SpringApplication.run(TerminalDevilalApplication.class, args);
	}
}
