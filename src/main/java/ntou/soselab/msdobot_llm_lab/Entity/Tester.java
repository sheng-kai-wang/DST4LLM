package ntou.soselab.msdobot_llm_lab.Entity;

import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;

import java.util.*;

public class Tester {

    private final String TESTER_ID;
    private String name;
    private Stack<String> intentNameStack = new Stack<>();
    private Map<String, Intent> intentMap = new HashMap<>();

    public Tester(String testerId, String name) {
        this.TESTER_ID = testerId;
        this.name = name;
    }

    public String getId() {
        return this.TESTER_ID;
    }

    public String getName() {
        return this.name;
    }

    public String updateIntent(JSONObject matchedIntentAndEntity, Properties capabilityYaml, Long expiredInterval) throws JSONException {
        Iterator intentIt = matchedIntentAndEntity.keys();
        while (intentIt.hasNext()) {
            String intentName = intentIt.next().toString();
            JSONObject matchedEntitiesJSON = matchedIntentAndEntity.getJSONObject(intentName);

            // out of scope
            if ("out_of_scope".equals(intentName)) {
                System.out.println("[DEBUG] OUT OF SCOPE");
                return "Sorry, the message you entered is beyond the scope of the capability.";
            }

            // only entity
            if ("no_intent".equals(intentName)) {
                Map<String, String> originalEntityMap = Objects.requireNonNull(getTopIntent()).getEntities();
                Iterator entityIt = matchedEntitiesJSON.keys();
                while (entityIt.hasNext()) {
                    String matchedEntityName = entityIt.next().toString();
                    String matchedEntityValue = matchedEntitiesJSON.getString(matchedEntityName);
                    if ("null".equals(matchedEntityValue) || matchedEntityValue == null) continue;
                    originalEntityMap.replace(matchedEntityName, matchedEntityValue);
                }
                System.out.println("[DEBUG] NO intent, ONLY entity");
                System.out.println("[DEBUG] update original intent: " + intentNameStack.peek());
                break;
            }

            // push new intent
            if (!intentNameStack.contains(intentName)) {
                Map<String, String> newEntityMap = new HashMap<>();
                List<String> allEntityName = (List<String>) capabilityYaml.get(intentName);
                for (String entityName : allEntityName) {
                    if (matchedEntitiesJSON.has(entityName)) {
                        String entityValue = matchedEntitiesJSON.get(entityName).toString();
                        newEntityMap.put(entityName, entityValue);
                    } else {
                        newEntityMap.put(entityName, null);
                    }
                }
                Long expiredTimestamp = System.currentTimeMillis() + expiredInterval;
                Intent newIntent = new Intent(intentName, expiredTimestamp, newEntityMap);
                intentNameStack.push(intentName);
                intentMap.put(intentName, newIntent);
                System.out.println("[DEBUG] push new intent: " + intentName);

                // update original intent
            } else {
                Map<String, String> originalEntityMap = intentMap.get(intentName).getEntities();
                Iterator entityIt = matchedEntitiesJSON.keys();
                if (!entityIt.hasNext()) {
                    System.out.println("[DEBUG] match original intent but NO EXTRACTED ENTITY");
                    continue;
                }
                while (entityIt.hasNext()) {
                    String matchedEntityName = entityIt.next().toString();
                    String matchedEntityValue = matchedEntitiesJSON.getString(matchedEntityName);
                    if ("null".equals(matchedEntityValue) || matchedEntityValue == null) continue;
                    originalEntityMap.replace(matchedEntityName, matchedEntityValue);
                }
                System.out.println("[DEBUG] update original intent: " + intentName);
            }

            // update the performable status of the intent
            Intent topIntent = getTopIntent();
            if (topIntent != null) {
                Map<String, String> topIntentEntityMap = topIntent.getEntities();
                if (!topIntentEntityMap.containsValue(null)) {
                    intentMap.get(intentName).preparePerform();
                }
            }
        }
        return "ok";
    }

    public boolean isWaitingForPerform() {
        return !intentNameStack.isEmpty();
    }

    public boolean canPerform() {
        if (!isWaitingForPerform()) return false;
        return Objects.requireNonNull(getTopIntent()).canPerform();
    }

    public Intent checkPerformInfo() {
        if (!canPerform()) return null;
        return getTopIntent();
    }

    public String performTopIntent() {
        if (!canPerform()) return null;
        String performedIntentName = intentNameStack.pop();
        intentMap.remove(performedIntentName);
        return performedIntentName;
    }

    public String cancelTopIntent() {
        if (!canPerform()) return null;
        String cancelledIntentName = intentNameStack.pop();
        intentMap.remove(cancelledIntentName);
        return cancelledIntentName;
    }

    public Intent getTopIntent() {
        if (!isWaitingForPerform()) return null;
        String topIntentName = intentNameStack.peek();
        return intentMap.get(topIntentName);
    }
}
