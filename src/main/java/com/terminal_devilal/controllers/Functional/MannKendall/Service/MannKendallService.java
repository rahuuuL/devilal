package com.terminal_devilal.controllers.Functional.MannKendall.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.terminal_devilal.controllers.Functional.MannKendall.Model.MannKendallResponse;

@Service
public class MannKendallService {

	@Value("${python.script.root}")
	private String pythonScriptRoot;

	// Relative path of Python script inside root folder
	private final String scriptRelativePath = "mann_kendall.py";

	private final ObjectMapper objectMapper = new ObjectMapper();

	private static final Logger log = LoggerFactory.getLogger(MannKendallService.class);

	public MannKendallResponse runMannKendall(List<Double> values) throws Exception {
		// Construct full Python script path
		String scriptFullPath = pythonScriptRoot + File.separator + scriptRelativePath;

		// Convert input data to JSON string
		String inputJson = objectMapper.writeValueAsString(values);

		// Build the process to execute Python script
		ProcessBuilder processBuilder = new ProcessBuilder("python3", scriptFullPath, inputJson);
		processBuilder.redirectErrorStream(true);
		Process process = processBuilder.start();

		// Read the output from the Python script
		BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
		StringBuilder outputBuilder = new StringBuilder();
		String line;
		while ((line = reader.readLine()) != null) {
			outputBuilder.append(line);
		}
		reader.close();

		// Read stderr (error messages)
		BufferedReader stdErrReader = new BufferedReader(new InputStreamReader(process.getErrorStream()));
		StringBuilder stdErrBuilder = new StringBuilder();
		while ((line = stdErrReader.readLine()) != null) {
			stdErrBuilder.append(line).append(System.lineSeparator());
		}
		stdErrReader.close();

		int exitCode = process.waitFor();
		if (exitCode != 0) {
			// Log full stderr output for diagnosis
			String errMsg = stdErrBuilder.toString();
			log.error("Python script error output: {}", errMsg);
			throw new RuntimeException("Python script exited with error code " + exitCode);

		}

		String output = outputBuilder.toString();

		// Parse the JSON output using Jackson
		JsonNode jsonResult = objectMapper.readTree(output);

		// Map JSON fields to response DTO
		MannKendallResponse response = new MannKendallResponse();
		response.setTrend(jsonResult.path("trend").asText(null));
		response.setH(jsonResult.has("h") ? jsonResult.get("h").asBoolean() : null);
		response.setP(jsonResult.has("p") ? jsonResult.get("p").asDouble() : null);
		response.setZ(jsonResult.has("z") ? jsonResult.get("z").asDouble() : null);
		response.setTau(jsonResult.has("Tau") ? jsonResult.get("Tau").asDouble() : null);
		response.setS(jsonResult.has("s") ? jsonResult.get("s").asDouble() : null);
		response.setVar_s(jsonResult.has("var_s") ? jsonResult.get("var_s").asDouble() : null);
		response.setSlope(jsonResult.has("slope") ? jsonResult.get("slope").asDouble() : null);
		response.setIntercept(jsonResult.has("intercept") ? jsonResult.get("intercept").asDouble() : null);

		return response;
	}
}
