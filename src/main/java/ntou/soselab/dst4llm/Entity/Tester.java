package ntou.soselab.dst4llm.Entity;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import ntou.soselab.dst4llm.Service.CapabilityLoader;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.*;

public class Tester {

    private final String TESTER_ID;
    private String name;
    private Stack<String> intentNameStack;
    private Map<String, Intent> intentMap;

    public Tester(String testerId, String name) {
        this.TESTER_ID = testerId;
        this.name = name;
        this.intentNameStack = new Stack<>();
        this.intentMap = new HashMap<>();
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
        System.out.println("[DEBUG] updateIntent()");
        Iterator intentIt = matchedIntentAndEntity.keys();
        while (intentIt.hasNext()) {
            String intentName = intentIt.next().toString();
            System.out.println("[Intent Name] " + intentName);
            JSONObject matchedEntitiesJSON = matchedIntentAndEntity.getJSONObject(intentName);
            System.out.println("[Matched Entities JSON] " + matchedEntitiesJSON);

            // out of scope
            if ("out_of_scope".equals(intentName) || "".equals(intentName)) {
                System.out.println("[DEBUG] OUT OF SCOPE");
                return "Sorry, the message you entered is beyond the scope of the capability.";
            }

            // only entity
            if ("no_intent".equals(intentName)) {
                if (getTopIntent() == null) continue;
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

                // update the performable status of the TOP intent
                Intent currentIntent = getTopIntent();
                updatePerformableStatusOfIntent(currentIntent);
                continue;
            }

            // push new intent
            if (!intentNameStack.contains(intentName)) {
                Map<String, String> newEntityMap = new HashMap<>();
                String allEntityNameJsonString;
                // ignore undefined intent
                try {
                    allEntityNameJsonString = capabilityLoader.getCapabilityByJsonPath("$." + intentName);
                } catch (Exception e) {
                    e.printStackTrace();
                    continue;
                }
                List allEntityNameList;
                try {
                    allEntityNameList = new ObjectMapper().readValue(allEntityNameJsonString, List.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                for (Object entityNameObj : allEntityNameList) {
                    String entityName = entityNameObj.toString();
                    if (matchedEntitiesJSON.has(entityName)) {
                        String entityValue = null;
                        Object entityValueObj = matchedEntitiesJSON.opt(entityName);
                        // avoid the value is JSONObject
                        if (entityValueObj instanceof String) {
                            entityValue = matchedEntitiesJSON.getString(entityName);
                        }
                        if (!isIgnoredEntity(entityValue)) {
                            newEntityMap.put(entityName, entityValue);
                            continue;
                        }
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
                    String matchedEntityValue = null;
                    Object matchedEntityValueObj = matchedEntitiesJSON.opt(matchedEntityName);
                    // avoid the value is JSONObject
                    if (matchedEntityValueObj instanceof String) {
                        matchedEntityValue = matchedEntitiesJSON.getString(matchedEntityName);
                    }
                    if (isIgnoredEntity(matchedEntityValue)) continue;
                    originalEntityMap.replace(matchedEntityName, matchedEntityValue);
                }
                intentMap.get(intentName).updateExpiredTimestamp(expiredInterval);
                System.out.println("[DEBUG] update original intent: " + intentName);
            }

            // update the performable status of the intent
            Intent currentIntent = intentMap.get(intentName);
            updatePerformableStatusOfIntent(currentIntent);
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
                "未提供".equals(entityValue) ||
                entityValue.startsWith("<");
    }

    private void updatePerformableStatusOfIntent(Intent intent) {
        Map<String, String> currentIntentEntityMap = intent.getEntities();
        System.out.println("[DEBUG] Current Intent's Entity Map:");
        System.out.println(currentIntentEntityMap);
        if (!currentIntentEntityMap.containsValue(null)) intent.preparePerform();
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
        if (!isWaitingForPerform()) {
            System.out.println("[DEBUG] NO Performable Intent");
            return performableIntentList;
        }
        System.out.println("[DEBUG] Performable Intent:");
        for (Intent intent : intentMap.values()) {
            if (intent.canPerform()) {
                System.out.println("[Intent Name] " + intent.getName());
                performableIntentList.add(intent);
            }
        }
        return performableIntentList;
    }

    public List<String> removeExpiredIntent() {
        ArrayList<String> removedIntentList = new ArrayList<>();
        if (!isWaitingForPerform()) return removedIntentList;
        // to avoid java.util.ConcurrentModificationException
        List<String> intentToRemoveList = new ArrayList<>();
        for (Intent intent : intentMap.values()) {
            if (System.currentTimeMillis() > intent.getExpiredTimestamp()) {
                String intentName = intent.getName();
                intentNameStack.remove(intentName);
                intentToRemoveList.add(intentName);
            }
        }
        for (String intentName : intentToRemoveList) {
            intentMap.remove(intentName);
            removedIntentList.add(intentName);
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
