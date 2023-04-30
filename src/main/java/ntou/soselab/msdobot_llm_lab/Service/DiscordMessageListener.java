package ntou.soselab.msdobot_llm_lab.Service;

import net.dv8tion.jda.api.entities.channel.ChannelType;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

@Service
public class DiscordMessageListener extends ListenerAdapter {

    private final String EXAMPLE_CHANNEL_ID;
    private final String DEMO_CHANNEL_ID;
    private final String GUILD_ID;

    public DiscordMessageListener(Environment env) {
        this.EXAMPLE_CHANNEL_ID = env.getProperty("discord.channel.example.id");
        this.DEMO_CHANNEL_ID = env.getProperty("discord.channel.demo.id");
        this.GUILD_ID = env.getProperty("discord.guild.id");
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        if (event.getAuthor().isBot()) return;

        if (shouldReply(event)) {
            String message = event.getMessage().getContentRaw();
            event.getChannel().sendMessage(message).queue();
        }
    }

    private boolean shouldReply(MessageReceivedEvent event) {
        if (!event.getGuild().getId().equals(GUILD_ID)) return false;
        String channelId = event.getChannel().getId();
        return event.isFromType(ChannelType.PRIVATE) || channelId.equals(EXAMPLE_CHANNEL_ID) || channelId.equals(DEMO_CHANNEL_ID);
    }
}