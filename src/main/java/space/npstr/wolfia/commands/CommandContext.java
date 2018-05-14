/*
 * Copyright (C) 2017 Dennis Neufeld
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package space.npstr.wolfia.commands;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.annotations.FieldsAreNonNullByDefault;
import space.npstr.annotations.ParametersAreNonnullByDefault;
import space.npstr.annotations.ReturnTypesAreNonNullByDefault;
import space.npstr.sqlsauce.DatabaseException;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.game.exceptions.IllegalGameStateException;
import space.npstr.wolfia.utils.discord.RestActions;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * Created by napster on 08.09.17.
 * <p>
 * Convenience container for values associated with an issued command, also does the parsing
 * <p>
 * Don't save these anywhere as they hold references to JDA objects, just pass them down through (short-lived) command execution
 */
@Slf4j
@FieldsAreNonNullByDefault
@ParametersAreNonnullByDefault
@ReturnTypesAreNonNullByDefault
public class CommandContext extends MessageContext {


    public final String trigger;                        // the command trigger, e.g. "play", or "p", or "pLaY", whatever the user typed
    public final String[] args;                         // the arguments split by whitespace, excluding prefix and trigger
    public final String rawArgs;                        // raw arguments excluding prefix and trigger, trimmed
    public final BaseCommand command;
//    private final Histogram.Timer received;             // time when we received this command

    /**
     * @param event
     *         the event to be parsed
     *
     * @return The full context for the triggered command, or null if it's not a command that we know.
     */
    @Nullable
    public static CommandContext parse(final MessageReceivedEvent event)//, final Histogram.Timer received)
            throws DatabaseException {

        final String raw = event.getMessage().getContentRaw();
        String input;

        final String prefix = WolfiaConfig.DEFAULT_PREFIX;
        if (raw.toLowerCase().startsWith(prefix.toLowerCase())) {
            input = raw.substring(prefix.length());
        } else {
            return null;
        }

        input = input.trim();// eliminate possible whitespace between the prefix and the rest of the input
        if (input.isEmpty()) {
            return null;
        }

        //split by any length of white space characters
        // the \p{javaSpaceChar} instead of the better known \s is used because it actually includes unicode whitespaces
        final String[] args = input.split("\\p{javaSpaceChar}+");
        if (args.length < 1) {
            return null; //while this shouldn't technically be possible due to the preprocessing of the input, better be safe than throw exceptions
        }

        final String commandTrigger = args[0];
        final BaseCommand command = CommRegistry.getRegistry().getCommand(commandTrigger.toLowerCase());

        if (command == null) {
            return null;
        } else {
            return new CommandContext(event, commandTrigger,
                    Arrays.copyOfRange(args, 1, args.length),//exclude args[0] that contains the command trigger
                    input.replaceFirst(Pattern.quote(commandTrigger), "").trim(),
                    command
            );
        }
    }

    protected CommandContext(final MessageReceivedEvent event, final String trigger, final String[] args,
                             final String rawArgs, final BaseCommand command) {
//                             final Histogram.Timer received) {
        super(event);
        this.trigger = trigger;
        this.args = args;
        this.rawArgs = rawArgs;
        this.command = command;
//        this.received = received;
    }


    public boolean hasArguments() {
        return this.args.length > 0 && !this.rawArgs.isEmpty();
    }

    /**
     * Deletes the users message that triggered this command, if we have the permissions to do so
     */
    public void deleteMessage() {
        final TextChannel tc = this.msg.getTextChannel();
        if (tc != null && tc.getGuild().getSelfMember().hasPermission(tc, Permission.MESSAGE_MANAGE)) {
            RestActions.deleteMessage(this.msg);
        }
    }

    //reply with the help
    public void help() {
        reply(this.command.formatHelp(this.invoker));
    }

    public boolean invoke() throws IllegalGameStateException, DatabaseException {
        final boolean success = this.command.execute(this);
//        this.received.observeDuration();
        return success;
    }

    /**
     * Transforms this context into a guild context, telling the invoker to run the command in a guild if requested
     *
     * @param answerUser
     *         set to false to not tell the invoker about running the command in a guild
     *         <p>
     *
     * @return a GuildCommandContext if this command was issued in a guild, null otherwise
     */
    @Nullable
    public GuildCommandContext requireGuild(final boolean... answerUser) {
        if (this.channel.getType() == ChannelType.TEXT) {
            final TextChannel tc = (TextChannel) this.channel;
            final Guild g = tc.getGuild();
            final Member m = this.event.getMember();
            if (m != null) {
                return new GuildCommandContext(this, g, m, tc);
            } else {
                log.warn("Uh oh member is unexpectedly null when transforming CommandContext to GuildCommandContext");
            }
        }

        if (answerUser.length == 0 || answerUser[0]) {
            replyWithMention("please run this command in a guild channel.");
        }
        return null;
    }
}
