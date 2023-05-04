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
                      DiscordGeneralListener generalListener,
                      DiscordSlashCommandListener slashCommandListener,
                      DiscordMessageListener messageListener,
                      DiscordButtonListener buttonListener) {

        String APP_TOKEN = env.getProperty("discord.application.token");
        JDABuilder.createDefault(APP_TOKEN)
                .enableIntents(GatewayIntent.MESSAGE_CONTENT)
                .enableIntents(GatewayIntent.GUILD_MEMBERS)
                .addEventListeners(generalListener)
                .addEventListeners(slashCommandListener)
                .addEventListeners(messageListener)
                .addEventListeners(buttonListener)
                .build();

        System.out.println();
        System.out.println("[DEBUG] JDA START!");
        System.out.println();
    }
}