//package com.terminal_devilal.configurations.Startup;
//
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.boot.CommandLineRunner;
//import org.springframework.stereotype.Component;
//
//@Component
//public class StartupRunner implements CommandLineRunner {
//
//	@Value("${app.kafka.script.path}")
//	private String kafkaScriptPath;
//
//	@Value("${app.python.script.path}")
//	private String pythonScriptPath;
//
//	@Override
//	public void run(String... args) throws Exception {
//		// Start Kafka server
//		ProcessBuilder kafkaProcess = new ProcessBuilder("cmd.exe", "/c", kafkaScriptPath);
//		kafkaProcess.inheritIO();
//		kafkaProcess.start();
//
//		// Start Python FastAPI server
//		ProcessBuilder pythonProcess = new ProcessBuilder("cmd.exe", "/c", pythonScriptPath);
//		pythonProcess.inheritIO();
//		pythonProcess.start();
//	}
//}
