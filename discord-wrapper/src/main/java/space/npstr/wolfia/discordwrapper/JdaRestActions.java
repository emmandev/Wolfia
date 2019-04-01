package space.npstr.wolfia.discordwrapper;

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Invite;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.exceptions.PermissionException;

import java.util.List;
import java.util.concurrent.CompletionStage;

/**
 * JDA implementation of Rest actions
 */
public class JdaRestActions implements DiscordRestActions {

    @Override
    public CompletionStage<String> getOrCreateInvite() {
        Guild guild = null;
        TextChannel channel = null;

        channel.createInvite().submit().toCompletableFuture()
                .thenApply(invite -> invite.getURL())
                .exceptionally(th -> {
                    
                })

        try {
            return channel.createInvite().complete().getURL();
        } catch (final PermissionException ignored) {
        }
        try {
            final List<Invite> invites = channel.getInvites().complete();
            if (!invites.isEmpty()) return invites.get(0).getURL();
        } catch (final PermissionException ignored) {
        }

        // if we reached this point, we failed at creating an invite for this channel
        if (onFail.length > 0) {
            onFail[0].execute();
        }
        return "";

        return null; //TODO
    }
}
