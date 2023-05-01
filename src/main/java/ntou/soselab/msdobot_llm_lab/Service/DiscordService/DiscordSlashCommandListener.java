package ntou.soselab.msdobot_llm_lab.Service.DiscordService;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;
import org.springframework.stereotype.Service;

@Service
public class DiscordSlashCommandListener extends ListenerAdapter {

    public void onSlashCommandInteraction(@NotNull SlashCommandInteractionEvent event) {
        if (event.isFromGuild()) return;
        String eventName = event.getName();
        if ("lab_start".equals(eventName)) {
            System.out.println(">>> trigger slash command event");

            System.out.println("[DEBUG] " + eventName);
            User user = event.getUser();
            System.out.println("[User ID] " + user.getId());
            System.out.println("[User Name] " + user.getName());
            //TODO: add the ID of user into dialogue tracker

            System.out.println("<<< end of current slash command event");
        }
    }
}
