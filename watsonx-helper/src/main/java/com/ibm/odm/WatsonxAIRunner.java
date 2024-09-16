package com.ibm.odm;

import java.util.Hashtable;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import com.mashape.unirest.http.Unirest;

/**
 * This class allows the preparation and invocation of a **watsonx.ai**
 * inference. It holds the definition of a template payload for REST API
 * end-point call along with the model parameters to run the inference, and also
 * the prompt template with the target variable values.
 * 
 * @author PIERREBerlandier
 *
 */
public class WatsonxAIRunner {
	//
	// Prompt template and variable values.
	//
	private String promptTemplate;
	private Hashtable<String, String> variables = new Hashtable<String, String>();
	//
	// Inference execution outcome.
	//
	private String output;
	private boolean error = false;
	private boolean exception = false;
	private String message;
	//
	// Model parameters: you can provide a default value for these parameters or set
	// their value as part of the ODM ruleflow using the WatsonxAIRunner setters.
	//
	private String projectId = "<your-watsonx-project-id>";
	private String modelId = "<your-favorite-llm>";
	private String decodingMethod = "greedy";
	private int maxNewTokens = 5;
	private int minNewTokens = 0;
	private double repetitionPenalty = 1.0;
	//
	// API end-point payload template.
	//
	private static String inputTemplate = "{\r\n" + " \"input\": \"%s\",\r\n" + " \"parameters\": {\r\n"
			+ "  \"decoding_method\": \"%s\",\r\n" + "  \"max_new_tokens\": %d,\r\n" + "  \"min_new_tokens\": %d,\r\n"
			+ "  \"stop_sequences\": [],\r\n" + "  \"repetition_penalty\": %f\r\n" + " },\r\n"
			+ " \"model_id\": \"%s\",\r\n" + " \"project_id\": \"%s\"\r\n" + "}";

	public String getDecodingMethod() {
		return decodingMethod;
	}

	public void setDecodingMethod(String decodingMethod) {
		this.decodingMethod = decodingMethod;
	}

	public int getMaxNewTokens() {
		return maxNewTokens;
	}

	public void setMaxNewTokens(int maxNewTokens) {
		this.maxNewTokens = maxNewTokens;
	}

	public int getMinNewTokens() {
		return minNewTokens;
	}

	public void setMinNewTokens(int minNewTokens) {
		this.minNewTokens = minNewTokens;
	}

	public double getRepetitionPenalty() {
		return repetitionPenalty;
	}

	public void setRepetitionPenalty(double repetitionPenalty) {
		this.repetitionPenalty = repetitionPenalty;
	}

	public String getModelId() {
		return modelId;
	}

	public void setModelId(String modelId) {
		this.modelId = modelId;
	}

	public String getProjectId() {
		return projectId;
	}

	public void setProjectId(String projectId) {
		this.projectId = projectId;
	}

	public String getPromptTemplate() {
		return promptTemplate;
	}

	public void setPromptTemplate(String promptTemplate) {
		this.promptTemplate = promptTemplate;
	}

	public String getOutput() {
		return output;
	}

	public boolean isError() {
		return error;
	}

	public boolean isException() {
		return exception;
	}

	public String getMessage() {
		return message;
	}

	public boolean isValid() {
		return !error && !exception;
	}

	public String getInput() {
		//
		// Replace variable placeholder.
		//
		String prompt = new String(promptTemplate);
		for (String variable : variables.keySet()) {
			prompt = prompt.replace(variable, variables.get(variable));
		}
		return String.format(inputTemplate, prompt, decodingMethod, maxNewTokens, minNewTokens, repetitionPenalty,
				modelId, projectId);
	}

	public void addVariable(String variable, String value) {
		variables.put(variable, value);
	}

	public void clearVariables() {
		variables.clear();
	}

	public String normalize(String response) {
		return response.trim().toLowerCase();
	}

	/**
	 * Runs the web service call with Unirest.
	 * 
	 * @param url       URL for the deployed watsonx.ai instance
	 * @param apiKey
	 * @param normalize If true, applies some post-processing on the output string.
	 */
	public void runInference(String url, String apiKey, boolean normalize) {
		Unirest.setTimeouts(0, 0);
		try {
			String request = getInput();
			String response = Unirest.post(url).header("Content-Type", "application/json")
					.header("Authorization", "Bearer " + getToken(apiKey)).body(request).asString().getBody();
			JSONObject jsonOutput = (JSONObject) (new JSONParser().parse(response));
			if (jsonOutput.get("error") != null) {
				error = true;
				message = jsonOutput.toJSONString();
			}
			JSONObject results = (JSONObject) ((JSONArray) jsonOutput.get("results")).get(0);
			output = (String) results.get("generated_text");
			if (normalize) {
				output = normalize(output);
			}
		} catch (Exception e) {
			output = null;
			exception = true;
			message = e.getMessage();
		}
	}

	/**
	 * Gets a bearer token for the web service execution.
	 * 
	 * @param apiKey
	 * @return
	 * @throws Exception
	 */
	private static String getToken(String apiKey) throws Exception {
		String queryParams = "grant_type=urn%3Aibm%3Aparams%3Aoauth%3Agrant-type%3Aapikey&apikey=" + apiKey;
		String url = "https://iam.cloud.ibm.com/identity/token" + "?" + queryParams;
		String response = Unirest.post(url).header("Content-Type", "application/x-www-form-urlencoded")
				.header("Accept", "application/json").asString().getBody();
		JSONObject jsonOutput = (JSONObject) (new JSONParser().parse(response));
		return (String) jsonOutput.get("access_token");
	}
}
