package ntou.soselab.chatops4msa_llm_lab.Service.DiscordService;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.utils.messages.MessageCreateData;
import ntou.soselab.chatops4msa_llm_lab.Service.NLPService.DialogueTracker;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class DiscordMessageListener extends ListenerAdapter {

    private final String GUILD_ID;
    private final String EXAMPLE_CHANNEL_ID;
    private DialogueTracker dialogueTracker;

    public DiscordMessageListener(Environment env, DialogueTracker dialogueTracker) {
        this.GUILD_ID = env.getProperty("discord.guild.id");
        this.EXAMPLE_CHANNEL_ID = env.getProperty("discord.channel.example.id");
        this.dialogueTracker = dialogueTracker;
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (shouldReply(event)) {

            System.out.println(">>> trigger message event");

            System.out.println("[TIME] " + new Date());

            event.getChannel().sendMessage("got it, processing...\n").queue();

            String userId = event.getAuthor().getId();
            String userName = event.getAuthor().getName();
            String userInput = event.getMessage().getContentRaw();

            System.out.println("[DEBUG] Receive Message");
            System.out.println("[User Name] " + userName);
            System.out.println("[User Input] " + userInput);

            try {
                MessageCreateData response = dialogueTracker.inputMessage(userId, userInput);
                event.getChannel().sendMessage(response).queue();

            } catch (Exception e) {
                String errorMessage = "```properties" + "\n[WARNING] Sorry, the system is currently overloaded with other requests.```";
                event.getChannel().sendMessage(errorMessage).queue();
                System.out.println("[Error] maybe the system is currently overloaded with other requests.");
                System.out.println(e.getMessage());
                e.printStackTrace();

            } finally {
                System.out.println("<<< end of current message event");
                System.out.println();
            }
        }
    }

    private boolean shouldReply(MessageReceivedEvent event) {
        if (event.isFromGuild() && !event.getGuild().getId().equals(GUILD_ID)) return false;
        String channelId = event.getChannel().getId();
        return event.isFromType(ChannelType.PRIVATE) || channelId.equals(EXAMPLE_CHANNEL_ID);
    }
}