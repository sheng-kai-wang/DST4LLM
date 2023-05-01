package ntou.soselab.msdobot_llm_lab.Service.NLPService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonParseException;
import ntou.soselab.msdobot_llm_lab.Entity.Intent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Service
public class DialogueTracker {

    private ConcurrentMap<String, Stack<Intent>> activeUserStacks;
    private final Long EXPIRED_INTERVAL;
    private ChatGPTService chatGPTService;

    @Autowired
    public DialogueTracker(Environment env, ChatGPTService chatGPTService) {
        this.activeUserStacks = new ConcurrentHashMap<>();
        this.EXPIRED_INTERVAL = Long.valueOf(Objects.requireNonNull(env.getProperty("intent.expired_time")));
        this.chatGPTService = chatGPTService;
    }

    public void addUser(String userId) {
        if (!hasUser(userId)) activeUserStacks.put(userId, new Stack<>());
    }

    public boolean hasUser(String userId) {
        return activeUserStacks.containsKey(userId);
    }

    public String inputMessage(String userId, String userInput) {
        if (chatGPTService.isPromptInjection(userInput)) {
            System.out.println("[WARNING] Prompt Injection");
            return "Sorry, the message you entered is beyond the scope of the capability.";
        }
        if (chatGPTService.isEndOfCapability(userInput)) {
            System.out.println("[DEBUG] End Of Capability");
            String cancelledCapability = cancelCapability(userId);
            if (cancelledCapability == null) return "There are no capabilities being performed yet.";
            return "Okay, we have cancelled the " + cancelledCapability + " capability for you.";
        }

        String errorMessage = "Sorry, the system has encountered a formatting exception.";
        try {
            JSONObject matchedIntentAndEntity = chatGPTService.classifyIntentAndExtractEntity(userInput);
            Iterator intentIt = matchedIntentAndEntity.keys();
            while (intentIt.hasNext()) {
                String intentName = intentIt.next().toString();
                Map<String, String> allEntitiesMap = new HashMap<>();
                List<String> allEntitiesName = (List<String>) chatGPTService.getCapabilityYaml().get(intentName);
                JSONObject matchedEntitiesObj = matchedIntentAndEntity.getJSONObject(intentName);
                for (String entityName : allEntitiesName) {
                    if (matchedEntitiesObj.has(entityName)) {
                        String entityValue = matchedEntitiesObj.get(entityName).toString();
                        allEntitiesMap.put(entityName, entityValue);
                    } else {
                        allEntitiesMap.put(entityName, null);
                    }
                }
                Long expiredTimestamp = System.currentTimeMillis() + EXPIRED_INTERVAL;
                Intent newIntent = new Intent(intentName, expiredTimestamp, allEntitiesMap);
                activeUserStacks.get(userId).push(newIntent);
                System.out.println("[DEBUG] push intent: " + intentName);
            }
            System.out.println("[DEBUG] push successful");
            return "ok!";

        } catch (JsonParseException e) {
            System.out.println("[ERROR] After ChatGPT -> json string to JSONObject exception");
            e.printStackTrace();
            return errorMessage;
        } catch (JsonProcessingException e) {
            System.out.println("[ERROR] Before ChatGPT -> yaml to json string exception");
            e.printStackTrace();
            return errorMessage;
        } catch (JSONException e) {
            System.out.println("[ERROR] After ChatGPT -> get JSONObject from JSONObject exception");
            e.printStackTrace();
            return errorMessage;
        }
    }

    private boolean isWaitingForPerform(String userId) {
        if (!hasUser(userId)) return false;
        Stack<Intent> intentStack = activeUserStacks.get(userId);
        return !intentStack.isEmpty();
    }

    public boolean canPerform(String userId) {
        if (!isWaitingForPerform(userId)) return false;
        return activeUserStacks.get(userId).peek().canPerform();
    }

    public String GenerateQuestion(String userId) {
        if (!isWaitingForPerform(userId)) return null;
        Intent waitingIntent = activeUserStacks.get(userId).peek();
        try {
            return chatGPTService.queryMissingParameter(waitingIntent.getName(), waitingIntent.getEntities());
        } catch (JSONException e) {
            System.out.println("[ERROR] Before ChatGPT -> yaml to JSONObject exception");
            e.printStackTrace();
            return "Sorry, the system has encountered a formatting exception.";
        }
    }

    public Intent checkPerformInfo(String userId) {
        if (!canPerform(userId)) return null;
        return activeUserStacks.get(userId).peek();
    }

    public void perform(String userId) {
        if (canPerform(userId)) activeUserStacks.get(userId).pop();
    }

    public String cancelCapability(String userId) {
        if (!isWaitingForPerform(userId)) return null;
        return activeUserStacks.get(userId).pop().getName();
    }
}
