package ntou.soselab.msdobot_llm_lab.Entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ntou.soselab.msdobot_llm_lab.Service.CapabilityLoader;
import org.springframework.boot.configurationprocessor.json.JSONArray;
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

    public String cancelTopIntent() {
        String topIntentName = getTopIntent().getName();
        intentNameStack.remove(topIntentName);
        intentMap.remove(topIntentName);
        return topIntentName;
    }

    public Intent getTopIntent() {
        if (!isWaitingForPerform()) return null;
        String topIntentName = intentNameStack.peek();
        return intentMap.get(topIntentName);
    }

    private boolean isWaitingForPerform() {
        return !intentNameStack.isEmpty();
    }

    public String updateIntent(JSONObject matchedIntentAndEntity, CapabilityLoader capabilityLoader, Long expiredInterval) throws JSONException {
        Iterator intentIt = matchedIntentAndEntity.keys();
        while (intentIt.hasNext()) {
            String intentName = intentIt.next().toString();
            JSONObject matchedEntitiesJSON = matchedIntentAndEntity.getJSONObject(intentName);

            // out of scope
            if ("out_of_scope".equals(intentName) || "".equals(intentName)) {
                System.out.println("[DEBUG] OUT OF SCOPE");
                return "Sorry, the message you entered is beyond the scope of the capability.";
            }

            // only entity
            if ("no_intent".equals(intentName)) {
                if (getTopIntent() == null) break;
                Map<String, String> originalEntityMap = getTopIntent().getEntities();
                Iterator entityIt = matchedEntitiesJSON.keys();
                while (entityIt.hasNext()) {
                    String matchedEntityName = entityIt.next().toString();
                    String matchedEntityValue = matchedEntitiesJSON.getString(matchedEntityName);
                    if (isIgnoredEntity(matchedEntityValue)) continue;
                    originalEntityMap.replace(matchedEntityName, matchedEntityValue);
                }
                getTopIntent().updateExpiredTimestamp(expiredInterval);
                System.out.println("[DEBUG] NO intent, ONLY entity");
                System.out.println("[DEBUG] update original intent: " + intentNameStack.peek());
                break;
            }

            // push new intent
            if (!intentNameStack.contains(intentName)) {
                Map<String, String> newEntityMap = new HashMap<>();
                String allEntityNameJsonString = capabilityLoader.getCapabilityByJsonPath("$." + intentName);
                List allEntityNameList;
                try {
                    allEntityNameList = new ObjectMapper().readValue(allEntityNameJsonString, List.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                for (Object entityNameObj : allEntityNameList) {
                    String entityName = entityNameObj.toString();
                    if (matchedEntitiesJSON.has(entityName)) {
                        String entityValue = matchedEntitiesJSON.get(entityName).toString();
                        if (!isIgnoredEntity(entityValue)) newEntityMap.put(entityName, entityValue);
                        continue;
                    }
                    newEntityMap.put(entityName, null);
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
                    if (isIgnoredEntity(matchedEntityValue)) continue;
                    originalEntityMap.replace(matchedEntityName, matchedEntityValue);
                }
                intentMap.get(intentName).updateExpiredTimestamp(expiredInterval);
                System.out.println("[DEBUG] update original intent: " + intentName);
            }

            // update the performable status of the intent
            Intent currentIntent = intentMap.get(intentName);
            Map<String, String> currentIntentEntityMap = currentIntent.getEntities();
            if (!currentIntentEntityMap.containsValue(null)) currentIntent.preparePerform();
        }

        System.out.println("[DEBUG] The intent map for " + this.name + " currently: ");
        System.out.println(getIntentMapString());
        return "ok\n";
    }

    private boolean isIgnoredEntity(String entityValue) {
        return entityValue == null ||
                entityValue.isEmpty() ||
                "null".equals(entityValue) ||
                "unspecified".equals(entityValue) ||
                entityValue.startsWith("<");
    }

    private String getIntentMapString() {
        StringBuilder intentSb = new StringBuilder();
        intentSb.append("{ ");
        for (Map.Entry<String, Intent> intentEntry : intentMap.entrySet()) {
            Map<String, String> entities = intentEntry.getValue().getEntities();
            StringBuilder entitySb = new StringBuilder();
            entitySb.append("[ ");
            for (Map.Entry<String, String> entityEntry : entities.entrySet()) {
                entitySb.append(entityEntry.getKey()).append(": ").append(entityEntry.getValue()).append(", ");
            }
            entitySb.delete(entitySb.length() - 2, entitySb.length());
            entitySb.append(" ]");

            intentSb.append(intentEntry.getKey()).append(": ").append(entitySb).append(", ");
        }
        intentSb.delete(intentSb.length() - 2, intentSb.length());
        intentSb.append(" }");
        return intentSb.toString();
    }

    public List<Intent> getPerformableIntentList() {
        ArrayList<Intent> performableIntentList = new ArrayList<>();
        if (!isWaitingForPerform()) return performableIntentList;
        for (Intent intent : intentMap.values()) {
            System.out.println("======= intent: " + intent.getName());
            System.out.println("======= intent: " + intent.getEntities());
            System.out.println("======= intent: " + intent.canPerform());
            if (intent.canPerform()) performableIntentList.add(intent);
        }
        System.out.println("========= performableIntentList: " + performableIntentList);
        return performableIntentList;
    }

    public List<String> removeExpiredIntent() {
        ArrayList<String> removedIntentList = new ArrayList<>();
        if (!isWaitingForPerform()) return removedIntentList;
        System.out.println("======== intentMap.keySet(): " + intentMap.keySet());
        System.out.println("===== intentMap.get('hotel_booking'): " + intentMap.get("hotel_booking"));
        for (Intent intent : intentMap.values()) {
            System.out.println("====== intent.getName(): " + intent.getName());
            if (System.currentTimeMillis() > intent.getExpiredTimestamp()) {
                String intentName = intent.getName();
                intentNameStack.remove(intentName);
                intentMap.remove(intentName);
                removedIntentList.add(intentName);
            }
        }
        return removedIntentList;
    }

    public ArrayList<String> performAllPerformableIntent() {
        List<Intent> performableIntentList = getPerformableIntentList();
        ArrayList<String> performedIntentNameList = new ArrayList<>();
        if (performableIntentList.isEmpty()) return null;
        for (Intent intent : performableIntentList) {
            String intentName = intent.getName();
            intentNameStack.remove(intentName);
            intentMap.remove(intentName);
            performedIntentNameList.add(intentName);
        }
        return performedIntentNameList;
    }

    public ArrayList<String> cancelAllPerformableIntent() {
        List<Intent> performableIntentList = getPerformableIntentList();
        ArrayList<String> cancelledIntentNameList = new ArrayList<>();
        if (performableIntentList.isEmpty()) return null;
        for (Intent intent : performableIntentList) {
            String intentName = intent.getName();
            intentNameStack.remove(intentName);
            intentMap.remove(intentName);
            cancelledIntentNameList.add(intentName);
        }
        return cancelledIntentNameList;
    }
}
