package ntou.soselab.msdobot_llm_lab.Service.NLPService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.google.gson.JsonParseException;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.messages.MessageCreateBuilder;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import ntou.soselab.msdobot_llm_lab.Entity.Intent;
import ntou.soselab.msdobot_llm_lab.Entity.Tester;
import ntou.soselab.msdobot_llm_lab.Service.CapabilityLoader;
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

    private ConcurrentMap<String, Tester> activeTesterMap = new ConcurrentHashMap<>();
    private List<String> waitingButtonTesterList = new ArrayList<>();
    private final Long EXPIRED_INTERVAL;
    private ChatGPTService chatGPTService;
    private final CapabilityLoader capabilityLoader;

    @Autowired
    public DialogueTracker(Environment env, ChatGPTService chatGPTService, CapabilityLoader capabilityLoader) {
        this.EXPIRED_INTERVAL = Long.valueOf(Objects.requireNonNull(env.getProperty("intent.expired_time")));
        this.chatGPTService = chatGPTService;
        this.capabilityLoader = capabilityLoader;
    }

    public MessageCreateData addTester(String testerId, String name) {
        MessageCreateBuilder mb = new MessageCreateBuilder();
        if (hasTester(testerId)) {
            return mb.setContent("```properties" + "\nYou have started the lab.```").build();
        } else {
            activeTesterMap.put(testerId, new Tester(testerId, name));
            return mb.setContent("<<TODO：實驗說明>>").build();
        }
    }

    private boolean hasTester(String testerId) {
        return activeTesterMap.containsKey(testerId);
    }

    public MessageCreateData inputMessage(String testerId, String testerInput) {
        Tester currentTester = activeTesterMap.get(testerId);
        MessageCreateBuilder mb = new MessageCreateBuilder();

        if (chatGPTService.isPromptInjection(testerInput)) {
            System.out.println("[WARNING] Prompt Injection");
            return mb.setContent("Sorry, the message you entered is beyond the scope of the capability.").build();
        }

        if (chatGPTService.isEndOfCapability(testerInput)) {
            System.out.println("[DEBUG] End Of Capability");
            String cancelledIntentName = currentTester.cancelTopIntent();
            if (cancelledIntentName == null) {
                return mb.setContent("There are no capabilities being prepared for perform yet.").build();
            }
            return mb.setContent("Okay, we have cancelled the " + cancelledIntentName + " capability for you.").build();
        }

        String errorMessage = "```properties" + "\nSorry, the system has encountered a formatting exception.```";
        try {
            JSONObject matchedIntentAndEntity = chatGPTService.classifyIntentAndExtractEntity(testerInput);
            Properties capabilityYaml = capabilityLoader.getCapabilityYaml();
            String response = currentTester.updateIntent(matchedIntentAndEntity, capabilityYaml, EXPIRED_INTERVAL);
            mb.addContent(response);
        } catch (JsonParseException e) {
            System.out.println("[ERROR] After ChatGPT -> json string to JSONObject exception");
            e.printStackTrace();
            return mb.setContent(errorMessage).build();
        } catch (JsonProcessingException e) {
            System.out.println("[ERROR] Before ChatGPT -> yaml to json string exception");
            e.printStackTrace();
            return mb.setContent(errorMessage).build();
        } catch (JSONException e) {
            System.out.println("[ERROR] After ChatGPT -> get JSONObject from JSONObject exception");
            e.printStackTrace();
            return mb.setContent(errorMessage).build();
        }

        // generate perform check (button)
        List<Intent> performableIntentList = currentTester.getPerformableIntentList();
        if (!performableIntentList.isEmpty()) {
            generatePerformCheck(mb, currentTester, performableIntentList);

            // generate question
        } else {
            removeExpiredIntent(currentTester);
            generateQuestion(mb, currentTester);
        }

        return mb.build();
    }

    private void generatePerformCheck(MessageCreateBuilder mb, Tester tester, List<Intent> performableIntentList) {
        mb.addContent("Here is the capability you are about to perform.");
        mb.addContent("Please use the BUTTON to indicate whether you want to proceed.");
        System.out.println("[DEBUG] generate perform check to " + tester.getName());
        for (Intent intent : performableIntentList) {
            String intentName = intent.getName();
            EmbedBuilder eb = new EmbedBuilder();
            eb.setTitle(intentName);
            System.out.println("[DEBUG][Intent] " + intentName);
            for (Map.Entry<String, String> entity : intent.getEntities().entrySet()) {
                String entityName = entity.getKey();
                String entityValue = entity.getValue();
                eb.addField(entityName, entityValue, false);
                System.out.println("[DEBUG][Entity] " + entityName + " = " + entityValue);
            }
            mb.addEmbeds(eb.build());
        }
        mb.addActionRow(Button.primary("", "Perform"));
        mb.addActionRow(Button.primary("", "Cancel"));
        waitingButtonTesterList.add(tester.getId());
        System.out.println("[DEBUG] Waiting for " + tester.getName() + "to click the button.");
    }

    public void removeWaitingTesterList(String testerId) {
        waitingButtonTesterList.remove(testerId);
    }

    public ArrayList<String> performAllPerformableIntent(String testerId) {
        return activeTesterMap.get(testerId).performAllPerformableIntent();
    }

    public ArrayList<String> cancelAllPerformableIntent(String testerId) {
        return activeTesterMap.get(testerId).cancelAllPerformableIntent();
    }

    private void removeExpiredIntent(Tester tester) {
        List<String> removedIntentList = activeTesterMap.get(tester.getId()).removeExpiredIntent();
        System.out.println("[DEBUG] remove expired intent of " + tester.getName());
        System.out.println("[DEBUG] removed intent list: " + removedIntentList);
    }

    private void generateQuestion(MessageCreateBuilder mb, Tester tester) {
        Intent waitingIntent = activeTesterMap.get(tester.getId()).getTopIntent();
        if (waitingIntent == null) return;
        System.out.println("[DEBUG] generate question to " + tester.getName());
        try {
            String question = chatGPTService.queryMissingParameter(waitingIntent.getName(), waitingIntent.getEntities());
            mb.addContent(question);
            System.out.println("[DEBUG][Question] " + question);
        } catch (JSONException e) {
            System.out.println("[ERROR] Before ChatGPT -> yaml to JSONObject exception");
            e.printStackTrace();
            mb.setContent("```properties" + "\nSorry, the system has encountered a formatting exception.```");
        }
    }

    public String generateQuestionString(String testerId) {
        Intent waitingIntent = activeTesterMap.get(testerId).getTopIntent();
        try {
            return chatGPTService.queryMissingParameter(waitingIntent.getName(), waitingIntent.getEntities());
        } catch (JSONException e) {
            System.out.println("[ERROR] Before ChatGPT -> yaml to JSONObject exception");
            e.printStackTrace();
            return "```properties" + "\nSorry, the system has encountered a formatting exception.```";
        }
    }
}
