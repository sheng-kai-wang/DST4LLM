package ntou.soselab.msdobot_llm_lab.Service.DiscordService;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ntou.soselab.msdobot_llm_lab.Service.NLPService.DialogueTracker;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class DiscordButtonListener extends ListenerAdapter {

    private DialogueTracker dialogueTracker;

    @Autowired
    public DiscordButtonListener(DialogueTracker dialogueTracker) {
        this.dialogueTracker = dialogueTracker;
    }

    @Override
    public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
        User tester = event.getUser();
        String testerId = tester.getId();
        System.out.println("[DEBUG] " + tester.getName() + " click " + event.getButton().getLabel());
        dialogueTracker.removeWaitingTesterList(testerId);
        String question = dialogueTracker.generateQuestionString(testerId);
        event.getHook().sendMessage("got it!\n\n" + question).queue();
    }
}
