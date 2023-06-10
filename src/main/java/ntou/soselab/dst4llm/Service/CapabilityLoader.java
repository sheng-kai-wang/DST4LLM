package ntou.soselab.dst4llm.Service;

import org.json.JSONException;
import org.json.JSONObject;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

@Service
public class CapabilityLoader {

    private final String CAPABILITY_FILE;
    private String capabilityJsonString;
    private String capabilityYamlStringForChatGPT;

    @Autowired
    public CapabilityLoader(Environment env) {
        this.CAPABILITY_FILE = env.getProperty("capability.file");
        this.capabilityJsonString = loadCapabilityYaml();
        this.capabilityYamlStringForChatGPT = loadCapabilityYamlStringForChatGPT();
    }

    public String getCapabilityByJsonPath(String jsonPath) {
        Object entityName = JsonPath.read(capabilityJsonString, jsonPath);
        if (entityName == null) return null;
        return entityName.toString();
    }

    public JSONObject getCapabilityJSONObject() throws JSONException {
        return new JSONObject(this.capabilityJsonString);
    }

    public String getCapabilityYamlStringForChatGPT() {
        return this.capabilityYamlStringForChatGPT;
    }

    private String loadCapabilityYaml() {
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
        return capabilityJsonString;
    }

    private String loadCapabilityYamlStringForChatGPT() {
        InputStream is;
        try {
            is = new ClassPathResource(CAPABILITY_FILE).getInputStream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        StringBuilder sb = new StringBuilder();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            String line;
            while ((line = br.readLine()) != null) {
                sb.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

//        sb.append("out_of_scope:");
        return sb.toString();
    }
}
