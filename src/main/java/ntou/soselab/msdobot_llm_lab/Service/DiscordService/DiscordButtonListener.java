package ntou.soselab.msdobot_llm_lab.Service.DiscordService;

import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import ntou.soselab.msdobot_llm_lab.Service.NLPService.DialogueTracker;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
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

        System.out.println(">>> trigger button interaction event");

        System.out.println("[TIME] " + new Date());

        User tester = event.getUser();
        String testerId = tester.getId();
        String testerName = tester.getName();
        String buttonId = event.getButton().getId();
        System.out.println("[DEBUG] " + testerName + " click " + buttonId);
        event.editButton(event.getButton().asDisabled()).queue();

        List<String> intentNameList = null;
        if ("Perform".equals(buttonId)) intentNameList = dialogueTracker.performAllPerformableIntent(testerId);
        if ("Cancel".equals(buttonId)) intentNameList = dialogueTracker.cancelAllPerformableIntent(testerId);

        if (dialogueTracker.isWaitingTester(testerId)) {
            dialogueTracker.removeWaitingTesterList(testerId);

            String question = dialogueTracker.generateQuestionString(testerId);
            event.getHook()
                    .sendMessage("got it!\n" + buttonId + " " + intentNameList + "\n\n" + question)
                    .queue();
            System.out.println("[DEBUG] generate question to " + testerName);
        }

        System.out.println("<<< end of current button interaction event");
        System.out.println();
    }
}