package ntou.soselab.msdobot_llm_lab.Service.DiscordService;

import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.PrivateChannel;
import net.dv8tion.jda.api.events.guild.member.GuildMemberJoinEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.DefaultMemberPermissions;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.FileCopyUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Service
public class DiscordGeneralListener extends ListenerAdapter {

    private final String WELCOME_FILE;

    @Autowired
    public DiscordGeneralListener(Environment env) {
        this.WELCOME_FILE = env.getProperty("welcome.file");
    }

    @Override
    public void onReady(@NotNull ReadyEvent event) {

        System.out.println(">>> trigger ready event");

        System.out.println("[TIME] " + new Date());

        event.getJDA()
                .upsertCommand("lab_start", "Start the lab")
                .queue(command -> {
                    command.editCommand()
                            .setGuildOnly(false)
                            .setDefaultPermissions(
                                    DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)).queue();
                });
        System.out.println("[DEBUG] Upsert Command lab_start");

        event.getJDA()
                .upsertCommand("lab_end", "End the lab")
                .queue(command -> {
                    command.editCommand()
                            .setGuildOnly(false)
                            .setDefaultPermissions(
                                    DefaultMemberPermissions.enabledFor(Permission.ADMINISTRATOR)).queue();
                });
        System.out.println("[DEBUG] Upsert Command lab_end");

        System.out.println("<<< end of current ready event");
        System.out.println();
    }

    @Override
    public void onGuildMemberJoin(@NotNull GuildMemberJoinEvent event) {

        System.out.println(">>> trigger guild member join event");

        User tester = event.getUser();
        System.out.println("[DEBUG] new tester: " + tester.getName());
        PrivateChannel channel = tester.openPrivateChannel().complete();
        channel.sendMessage(loadWelcomeMessage()).queue();

        System.out.println("<<< end of current guild member join event");
        System.out.println();
    }

    private String loadWelcomeMessage() {
        ClassPathResource resource = new ClassPathResource(WELCOME_FILE);
        byte[] bytes;
        try {
            bytes = FileCopyUtils.copyToByteArray(resource.getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return new String(bytes, StandardCharsets.UTF_8);
    }
}
