package ntou.soselab.msdobot_llm_lab.Service.DiscordService;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ntou.soselab.msdobot_llm_lab.Service.NLPService.DialogueTracker;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

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
        String testerName = tester.getName();
        String buttonLabel = event.getButton().getLabel();
        System.out.println("[DEBUG] " + testerName + " click " + buttonLabel);

        List<String> intentNameList = null;
        if ("Perform".equals(buttonLabel)) intentNameList = dialogueTracker.performAllPerformableIntent(testerId);
        if ("Cancel".equals(buttonLabel)) intentNameList = dialogueTracker.cancelAllPerformableIntent(testerId);
        dialogueTracker.removeWaitingTesterList(testerId);

        String question = dialogueTracker.generateQuestionString(testerId);
        event.editButton(event.getButton().asDisabled()).queue();
        event.getHook()
                .sendMessage("got it!\n" + buttonLabel + " " + intentNameList + "\n\n" + question)
                .queue();
        System.out.println("[DEBUG] generate question to " + testerName);
    }
}