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
        loadCapabilityYaml();
        loadCapabilityYamlStringForChatGPT();
    }

    public String getCapabilityByJsonPath(String jsonPath) {
        return JsonPath.read(capabilityJsonString, jsonPath).toString();
    }

    public JSONObject getCapabilityJSONObject() throws JSONException {
        return new JSONObject(this.capabilityJsonString);
    }

    public String getCapabilityYamlStringForChatGPT() {
        return this.capabilityYamlStringForChatGPT;
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

    private void loadCapabilityYamlStringForChatGPT() {
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
        this.capabilityYamlStringForChatGPT = sb.toString();
    }
}