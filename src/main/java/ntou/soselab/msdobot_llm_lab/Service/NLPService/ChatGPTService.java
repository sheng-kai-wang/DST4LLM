package ntou.soselab.msdobot_llm_lab.Service.NLPService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonParseException;
import ntou.soselab.msdobot_llm_lab.Service.CapabilityLoader;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class ChatGPTService {

    private final String OPENAI_API_URL;
    private final String OPENAI_API_KEY;
    private final String OPENAI_API_MODEL;

    private final String PROMPT_INJECTION_DETECTION_FILE;
    private final String END_OF_TOPIC_FILE;
    private final String INTENT_CLASSIFICATION_AND_ENTITY_EXTRACTION_FILE;
    private final String QUERYING_MISSING_PARAMETERS_FILE;

    private CapabilityLoader capabilityLoader;

    @Autowired
    public ChatGPTService(Environment env, CapabilityLoader capabilityLoader) {
        this.OPENAI_API_URL = env.getProperty("openai.api.url");
        this.OPENAI_API_KEY = env.getProperty("openai.api.key");
        this.OPENAI_API_MODEL = env.getProperty("openai.api.model");

        this.PROMPT_INJECTION_DETECTION_FILE = loadSystemPrompt(env.getProperty("prompts.prompt_injection_detection.file"));
        this.END_OF_TOPIC_FILE = loadSystemPrompt(env.getProperty("prompts.end_of_topic.file"));
        this.INTENT_CLASSIFICATION_AND_ENTITY_EXTRACTION_FILE = loadSystemPrompt(env.getProperty("prompts.intent_classification_and_entity_extraction.file"));
        this.QUERYING_MISSING_PARAMETERS_FILE = loadSystemPrompt(env.getProperty("prompts.querying_missing_parameters.file"));

        this.capabilityLoader = capabilityLoader;
    }

    public boolean isPromptInjection(String userPrompt) {
        System.out.println("[DEBUG] trigger isPromptInjection()");
        System.out.println("[User Prompt] " + userPrompt);

        String completion = inference(PROMPT_INJECTION_DETECTION_FILE, userPrompt);

        boolean isPromptInjection = completion.contains("true");
        System.out.println("[Is Prompt Injection?] " + isPromptInjection);
        return isPromptInjection;
    }

    public boolean isEndOfTopic(String userPrompt) {
        System.out.println("[DEBUG] trigger isEndOfTopic()");
        System.out.println("[User Prompt] " + userPrompt);

        String completion = inference(END_OF_TOPIC_FILE, userPrompt);

        boolean isEndOfTopic = completion.contains("true");
        System.out.println("[Is End Of Topic?] " + isEndOfTopic);
        return isEndOfTopic;
    }

    /**
     * @param userPrompt like "I would like to make a reservation at Noblesse Seafood Restaurant - Ocean University Branch and also book a flight at 10 a.m."
     * @return like {"restaurant_ordering": {"name_of_restaurant": "Noblesse Seafood Restaurant - Ocean University Branch"}, "flight_ticket_booking": {"time": "10 a.m."}}
     * @throws JsonProcessingException Before ChatGPT -> yaml to json string exception
     * @throws JsonParseException      After ChatGPT -> json string to JSONObject exception
     */
    public JSONObject classifyIntentAndExtractEntity(String userPrompt) throws JsonProcessingException, JsonParseException, JSONException {
        System.out.println("[DEBUG] trigger classifyIntentAndExtractEntity()");
        System.out.println("[User Prompt] " + userPrompt);

        String capabilityYamlStringForChatGPT = capabilityLoader.getCapabilityYamlStringForChatGPT();
        String systemPrompt = INTENT_CLASSIFICATION_AND_ENTITY_EXTRACTION_FILE.replace("<CAPABILITY_JSON>", capabilityYamlStringForChatGPT);

        String completion = inference(systemPrompt, userPrompt);
        System.out.println("[Completion String] " + completion);

        JSONObject completionJSON = new JSONObject(completion);
        System.out.println("[Completion JSON] " + completionJSON);
        return completionJSON;
    }

    /**
     * @param intentName
     * @param providedEntities
     * @return
     * @throws JSONException Before ChatGPT -> yaml to JSONObject exception
     */
    public String queryMissingParameter(String intentName, Map<String, String> providedEntities) throws JSONException {
        System.out.println("[DEBUG] trigger queryMissingParameter()");

        String systemPrompt = QUERYING_MISSING_PARAMETERS_FILE.replace("<INTENT_NAME>", intentName);

        StringBuilder providedEntityDescription = new StringBuilder();
        StringBuilder missingEntityDescription = new StringBuilder();
        JSONArray allEntities = capabilityLoader.getCapabilityJSONObject().getJSONArray(intentName);
        for (int i = 0; i < allEntities.length(); i++) {
            String currentEntityName = allEntities.getString(i);
            if (providedEntities.containsKey(currentEntityName)) {
                String entityValue = providedEntities.get(currentEntityName);
                providedEntityDescription.append("\"").append(currentEntityName).append("\" as ");
                providedEntityDescription.append("\"").append(entityValue).append("\", ");
            } else {
                missingEntityDescription.append("\"").append(currentEntityName).append("\", ");
            }
        }

        systemPrompt = systemPrompt.replace("<PROVIDED_ENTITY_DESCRIPTION>", providedEntityDescription.toString());
        systemPrompt = systemPrompt.replace("<MISSING_ENTITY_DESCRIPTION>", missingEntityDescription.toString());
        System.out.println("[System Prompt] " + systemPrompt);

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

        HttpEntity<String> requestEntity = new HttpEntity<>(requestBody.toString(), headers);

        ResponseEntity<String> responseEntity = restTemplate.exchange(OPENAI_API_URL, HttpMethod.POST, requestEntity, String.class);
        String completionString;
        try {
            JSONObject completionJSON = new JSONObject(responseEntity.getBody());
            int usedTokens = completionJSON.getJSONObject("usage").getInt("total_tokens");
            System.out.println("[Used Token] " + usedTokens);
            completionString = completionJSON
                    .getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

        } catch (JSONException e) {
            throw new RuntimeException(e);
        }
        return completionString;
    }

    private String loadSystemPrompt(String promptFile) {
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
