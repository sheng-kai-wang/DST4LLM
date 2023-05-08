package ntou.soselab.msdobot_llm_lab.Entity;

import java.util.Map;

public class Intent {

    private final String NAME;
    private Map<String, String> entities;
    private Long expiredTimestamp;
    private boolean canPerform;

    public Intent(String name, Long expiredTimestamp, Map<String, String> entities) {
        this.NAME = name;
        this.entities = entities;
        this.expiredTimestamp = expiredTimestamp;
        this.canPerform = false;
    }

    public String getName() {
        return this.NAME;
    }

    public Map<String, String> getEntities() {
        return this.entities;
    }

    public Long getExpiredTimestamp() {
        return this.expiredTimestamp;
    }

    public void updateExpiredTimestamp(Long expiredInterval) {
        this.expiredTimestamp = System.currentTimeMillis() + expiredInterval;
    }

    public boolean canPerform() {
        return this.canPerform;
    }

    public void preparePerform() {
        this.canPerform = true;
    }
}