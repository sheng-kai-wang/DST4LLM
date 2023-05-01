package ntou.soselab.msdobot_llm_lab.Entity;

import java.util.Map;

public class Intent {

    private final String NAME;
    private final Long EXPIRED_TIMESTAMP;
    private boolean canPerform = false;
    private Map<String, String> entities;

    public Intent(String name, Long expiredTimestamp, Map<String, String> entities) {
        this.NAME = name;
        this.EXPIRED_TIMESTAMP = expiredTimestamp;
        this.entities = entities;
    }

    public String getName() {
        return this.NAME;
    }

    public boolean canPerform() {
        return this.canPerform;
    }

    public void preparePerform() {
        this.canPerform = true;
    }

    public Map<String, String> getEntities() {
        return this.entities;
    }
}
