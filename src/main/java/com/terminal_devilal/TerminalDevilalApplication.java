package com.terminal_devilal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.terminal_devilal.configurations.KafkaProducerService;

@SpringBootApplication
public class TerminalDevilalApplication {
	
    @Autowired
    private static KafkaProducerService producerService;
    

	public static void main(String[] args) {
		SpringApplication.run(TerminalDevilalApplication.class, args);
	}
	
    public void run(String... args) throws Exception {
        producerService.sendMessage("test-topic", "Hello Kafka!");
    }
}
