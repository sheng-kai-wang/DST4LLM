package ntou.soselab.chatops4msa_llm_lab;

import ntou.soselab.chatops4msa_llm_lab.Service.NLPService.ChatGPTService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
class ChatOps4MsaLlmLabApplicationTests {

	private Environment env;
	private ChatGPTService chatGPTService;

	@Autowired
	public ChatOps4MsaLlmLabApplicationTests(Environment env, ChatGPTService chatGPTService) {
		this.env = env;
		this.chatGPTService = chatGPTService;
	}

	@Test
	void test() {
	}

}
