package ntou.soselab.msdobot_llm_lab.Entity;

import java.util.Map;

public class Intent {

    private final String NAME;
    private Map<String, String> entities;
    private final Long EXPIRED_TIMESTAMP;
    private boolean canPerform = false;

    public Intent(String name, Long expiredTimestamp, Map<String, String> entities) {
        this.NAME = name;
        this.EXPIRED_TIMESTAMP = expiredTimestamp;
        this.entities = entities;
    }

    public String getName() {
        return this.NAME;
    }

    public Map<String, String> getEntities() {
        return this.entities;
    }

    public Long getExpiredTimestamp() {
        return this.EXPIRED_TIMESTAMP;
    }

    public boolean canPerform() {
        return this.canPerform;
    }

    public void preparePerform() {
        this.canPerform = true;
    }
}