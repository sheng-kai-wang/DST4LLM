package ntou.soselab.msdobot_llm_lab.Service.DiscordService;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.requests.GatewayIntent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class JDAService {

    @Autowired
    public JDAService(Environment env,
                      DiscordSlashCommandListener slashCommandListener,
                      DiscordMessageListener messagelistener,
                      DiscordButtonListener buttonListener) {

        String APP_TOKEN = env.getProperty("discord.application.token");
        JDABuilder.createDefault(APP_TOKEN)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .addEventListeners(slashCommandListener)
                .addEventListeners(messagelistener)
                .addEventListeners(buttonListener)
                .build();

        System.out.println("[DEBUG] JDA START!");
    }
}