package ntou.soselab.msdobot_llm_lab.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.JsonParseException;
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
import java.util.Iterator;
import java.util.Properties;

@Service
public class ChatGPTService {

    private final String OPENAI_API_URL;
    private final String OPENAI_API_KEY;
    private final String OPENAI_API_MODEL;
    private final String PROMPT_INJECTION_DETECTION_FILE;
    private final String END_OF_CAPABILITY_FILE;
    private final String INTENT_CLASSIFICATION_AND_ENTITY_EXTRACTION_FILE;
    private final String QUERYING_MISSING_PARAMETERS_FILE;
    private final String CAPABILITY_FILE;

    @Autowired
    public ChatGPTService(Environment env) {
        this.OPENAI_API_URL = env.getProperty("openai.api.url");
        this.OPENAI_API_KEY = env.getProperty("openai.api.key");
        this.OPENAI_API_MODEL = env.getProperty("openai.api.model");

        this.PROMPT_INJECTION_DETECTION_FILE = env.getProperty("prompts.prompt_injection_detection.file");
        this.END_OF_CAPABILITY_FILE = env.getProperty("prompts.end_of_capability.file");
        this.INTENT_CLASSIFICATION_AND_ENTITY_EXTRACTION_FILE = env.getProperty("prompts.intent_classification_and_entity_extraction.file");
        this.QUERYING_MISSING_PARAMETERS_FILE = env.getProperty("prompts.querying_missing_parameters.file");

        this.CAPABILITY_FILE = env.getProperty("capability.file");
    }

    public boolean isPromptInjection(String userPrompt) {
        String systemPrompt = getSystemPrompt(PROMPT_INJECTION_DETECTION_FILE);
        String completion = inference(systemPrompt, userPrompt);
        return completion.contains("true");
    }

    public boolean isEndOfCapability(String userPrompt) {
        String systemPrompt = getSystemPrompt(END_OF_CAPABILITY_FILE);
        String completion = inference(systemPrompt, userPrompt);
        return completion.contains("true");
    }

    public JSONObject classifyIntentAndExtractEntity(String userPrompt) throws JsonProcessingException, JsonParseException {
        String systemPrompt = getSystemPrompt(INTENT_CLASSIFICATION_AND_ENTITY_EXTRACTION_FILE);

        Properties capabilities = getCapabilityYaml();
        String capabilityJson = new ObjectMapper().writeValueAsString(capabilities);

        systemPrompt = systemPrompt.replace("<CAPABILITY_JSON>", capabilityJson);

        String completion = inference(systemPrompt, userPrompt);
        return new Gson().fromJson(completion, JSONObject.class);
    }

    public String queryMissingParameter(String intent, JSONObject providedEntities) throws JSONException {
        String systemPrompt = getSystemPrompt(QUERYING_MISSING_PARAMETERS_FILE);
        systemPrompt = systemPrompt.replace("<INTENT_NAME>", intent);

        JSONObject capabilityJSON = new JSONObject(getCapabilityYaml());
        JSONArray allEntities = capabilityJSON.getJSONArray(intent);

        StringBuilder providedEntityDescription = new StringBuilder();
        StringBuilder missingEntityDescription = new StringBuilder();
        for (int i = 0; i < allEntities.length(); i++) {
            String currentEntityName = allEntities.getString(i);
            if (providedEntities.has(currentEntityName)) {
                String entityValue = providedEntities.getString(currentEntityName);
                providedEntityDescription.append("\"").append(currentEntityName).append("\" as ");
                providedEntityDescription.append("\"").append(entityValue).append("\", ");
            } else {
                missingEntityDescription.append("\"").append(currentEntityName).append("\", ");
            }
        }

        systemPrompt = systemPrompt.replace("<PROVIDED_ENTITY_DESCRIPTION>", providedEntityDescription.toString());
        systemPrompt = systemPrompt.replace("<MISSING_ENTITY_DESCRIPTION>", missingEntityDescription.toString());

        return inference(systemPrompt, null);
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

    private Properties getCapabilityYaml() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(CAPABILITY_FILE));
        return yaml.getObject();
    }

    private String getSystemPrompt(String promptFile) {
        ClassPathResource resource = new ClassPathResource(promptFile);
        byte[] bytes;
        try {
            bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
