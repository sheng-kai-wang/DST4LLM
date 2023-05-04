package ntou.soselab.msdobot_llm_lab.Service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.configurationprocessor.json.JSONException;
import org.springframework.boot.configurationprocessor.json.JSONObject;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;

@Service
public class CapabilityLoader {

    private final String CAPABILITY_FILE;
    private String capabilityJsonString;

    @Autowired
    public CapabilityLoader(Environment env) {
        this.CAPABILITY_FILE = env.getProperty("capability.file");
        loadCapabilityYaml();
    }

    public String getCapabilityByJsonPath(String jsonPath) {
        return JsonPath.read(capabilityJsonString, jsonPath).toString();
    }

    public JSONObject getCapabilityJSONObject() throws JSONException {
        return new JSONObject(this.capabilityJsonString);
    }

    private void loadCapabilityYaml() {
        InputStream is;
        Object yamlObj;
        String capabilityJsonString;

        try {
            is = new ClassPathResource(CAPABILITY_FILE).getInputStream();
            yamlObj = new Yaml().load(is);
            capabilityJsonString = new ObjectMapper().writeValueAsString(yamlObj);

        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("[DEBUG] all capability:");
        System.out.println(yamlObj);
        System.out.println();
        this.capabilityJsonString = capabilityJsonString;
    }
}
