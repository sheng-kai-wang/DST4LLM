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
            Intent currentIntent = intentMap.get(intentName);
            Map<String, String> currentIntentEntityMap = currentIntent.getEntities();
            if (!currentIntentEntityMap.containsValue(null)) currentIntent.preparePerform();
        }

        return "ok";
    }

    public List<Intent> getPerformableIntentList() {
        ArrayList<Intent> performableIntentList = new ArrayList<>();
        if (!isWaitingForPerform()) return performableIntentList;
        for (Intent intent : intentMap.values()) {
            if (intent.canPerform()) performableIntentList.add(intent);
        }
        return performableIntentList;
    }

    public List<String> removeExpiredIntent() {
        ArrayList<String> removedIntentList = new ArrayList<>();
        if (!isWaitingForPerform()) return removedIntentList;
        for (Intent intent : intentMap.values()) {
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
