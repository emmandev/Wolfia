package space.npstr.wolfia.discordwrapper;

import javax.annotation.CheckReturnValue;
import java.util.concurrent.CompletionStage;

/**
 * Execute calls against DDoScord's Rest Api
 */
public interface DiscordRestActions {

    /**
     * @return the url of the invite
     */
    @CheckReturnValue
    CompletionStage<String> getOrCreateInvite();

}
