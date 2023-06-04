package ntou.soselab.dst4llm;

import ntou.soselab.dst4llm.Service.NLPService.ChatGPTService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;

@SpringBootTest
class DST4LLMApplicationTests {

	private Environment env;
	private ChatGPTService chatGPTService;

	@Autowired
	public DST4LLMApplicationTests(Environment env, ChatGPTService chatGPTService) {
		this.env = env;
		this.chatGPTService = chatGPTService;
	}

	@Test
	void test() {
	}

}
