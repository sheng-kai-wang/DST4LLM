package ntou.soselab.msdobot_llm_lab.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONArray;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

@Service
public class ChatGPTService {

    private final String OPENAI_API_URL;
    private final String OPENAI_API_KEY;
    private final String OPENAI_API_MODEL;
    private final String PROMPT_INJECTION_DETECTION_URL;
    private final String END_OF_CAPABILITY_URL;
    private final String INTENT_CLASSIFICATION_AND_ENTITY_EXTRACTION_URL;
    private final String QUERYING_MISSING_PARAMETERS_URL;
    private final String CAPABILITY_FILE;

    @Autowired
    public ChatGPTService(Environment env) {
        this.OPENAI_API_URL = env.getProperty("openai.api.url");
        this.OPENAI_API_KEY = env.getProperty("openai.api.key");
        this.OPENAI_API_MODEL = env.getProperty("openai.api.model");
        this.PROMPT_INJECTION_DETECTION_URL = env.getProperty("prompts.prompt_injection_detection.url");
        this.END_OF_CAPABILITY_URL = env.getProperty("prompts.end_of_capability.url");
        this.INTENT_CLASSIFICATION_AND_ENTITY_EXTRACTION_URL = env.getProperty("prompts.intent_classification_and_entity_extraction.url");
        this.QUERYING_MISSING_PARAMETERS_URL = env.getProperty("prompts.querying_missing_parameters.url");

        this.CAPABILITY_FILE = env.getProperty("capability.file");
    }

    public boolean isPromptInjection(String userPrompt) {
        String systemPrompt = systemPromptReader(PROMPT_INJECTION_DETECTION_URL);
        String completion = inference(systemPrompt, userPrompt);
        return completion.contains("true");
    }

    private String inference(String systemPrompt, String userPrompt) {
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + OPENAI_API_KEY);

        JSONObject requestBody = new JSONObject();
        try {
            requestBody.put("model", OPENAI_API_MODEL);
            requestBody.put("temperature", 0);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        JSONArray messages = new JSONArray();

        JSONObject systemMessage = new JSONObject();
        try {
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        messages.put(systemMessage);

        if (userPrompt != null) {
            JSONObject userMessage = new JSONObject();
            try {
                userMessage.put("role", "user");
                userMessage.put("content", userPrompt);
            } catch (JSONException e) {
                throw new RuntimeException(e);
            }
            messages.put(userMessage);
        }

        try {
            requestBody.put("messages", messages);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        HttpEntity<String> entity = new HttpEntity<>(requestBody.toString(), headers);

        return restTemplate.postForObject(OPENAI_API_URL, entity, String.class);
    }

    private Properties capabilityReader() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(CAPABILITY_FILE));
        return yaml.getObject();
    }

    private String systemPromptReader(String promptUrl) {
        ClassPathResource resource = new ClassPathResource(promptUrl);
        byte[] bytes;
        try {
            bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
