package ntou.soselab.msdobot_llm_lab;

import ntou.soselab.msdobot_llm_lab.Service.NLPService.ChatGPTService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
class MsdoBotLlmLabApplicationTests {

	private Environment env;
	private ChatGPTService chatGPTService;

	@Autowired
	public MsdoBotLlmLabApplicationTests(Environment env, ChatGPTService chatGPTService) {
		this.env = env;
		this.chatGPTService = chatGPTService;
	}

	@Test
	void test() {
	}

}
