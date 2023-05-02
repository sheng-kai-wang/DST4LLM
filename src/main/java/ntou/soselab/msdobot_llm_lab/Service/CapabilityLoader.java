package ntou.soselab.msdobot_llm_lab.Service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.YamlPropertiesFactoryBean;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.util.Properties;

@Service
public class CapabilityLoader {

    private final String CAPABILITY_FILE;
    private Properties capabilityYaml;

    @Autowired
    public CapabilityLoader(Environment env) {
        this.CAPABILITY_FILE = env.getProperty("capability.file");
        loadCapabilityYaml();
    }

    public Properties getCapabilityYaml() {
        return this.capabilityYaml;
    }

    private void loadCapabilityYaml() {
        YamlPropertiesFactoryBean yaml = new YamlPropertiesFactoryBean();
        yaml.setResources(new ClassPathResource(CAPABILITY_FILE));
        Properties capabilityYaml = yaml.getObject();
        System.out.println("[DEBUG] all capability:");
        System.out.println(capabilityYaml);
        System.out.println();
        this.capabilityYaml = capabilityYaml;
    }
}
