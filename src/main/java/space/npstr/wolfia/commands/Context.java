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


import net.dv8tion.jda.core.EmbedBuilder;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.MessageEmbed;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import space.npstr.annotations.FieldsAreNonNullByDefault;
import space.npstr.annotations.ParametersAreNonnullByDefault;
import space.npstr.annotations.ReturnTypesAreNonNullByDefault;
import space.npstr.sqlsauce.entities.discord.DiscordUser;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.utils.discord.RestActions;
import space.npstr.wolfia.utils.discord.TextchatUtils;

import javax.annotation.CheckReturnValue;
import javax.annotation.Nullable;
import java.awt.*;
import java.util.Optional;
import java.util.function.Consumer;

/**
 * Created by napster on 10.09.17.
 * <p>
 * Provides a context to whats going on. Where is it happening, who caused it?
 * Also home to a bunch of convenience methods to send replies via RestActions.
 */
@SuppressWarnings("unused")
@FieldsAreNonNullByDefault
@ParametersAreNonnullByDefault
@ReturnTypesAreNonNullByDefault
public interface Context {

    // Always present values of a context:

    @CheckReturnValue
    MessageChannel getChannel();

    @CheckReturnValue
    User getInvoker();

    //message that triggered this context
    @CheckReturnValue
    Message getMessage();


    // Common, but optional values of a context:

    @CheckReturnValue
    Optional<Member> getMember();

    @CheckReturnValue
    Optional<Guild> getGuild();

    // ********************************************************************************
    //                         Convenience reply methods
    // ********************************************************************************
    // NOTE: they all try to end up in the reply0 method for consistent behaviour


    default void reply(final MessageEmbed embed) {
        reply0(RestActions.from(embed), null);
    }

    default void reply(final EmbedBuilder eb) {
        reply(eb.build());
    }

    default void reply(final Message message, @Nullable final Consumer<Message> onSuccess) {
        reply0(message, onSuccess);
    }

    default void reply(final String message, @Nullable final Consumer<Message> onSuccess) {
        reply(RestActions.getMessageBuilder().append(message).build(), onSuccess);
    }

    default void reply(final Message message) {
        reply(message, null);
    }

    default void reply(final String message) {
        reply(RestActions.getMessageBuilder().append(message).build(), null);
    }

    default void replyWithName(final String message, @Nullable final Consumer<Message> onSuccess) {
        reply(TextchatUtils.prefaceWithName(getEffectiveName(), message, true), onSuccess);
    }

    default void replyWithName(final String message) {
        replyWithName(message, null);
    }

    default void replyWithMention(final String message, @Nullable final Consumer<Message> onSuccess) {
        reply(TextchatUtils.prefaceWithMention(getInvoker(), message), onSuccess);
    }

    default void replyWithMention(final String message) {
        replyWithMention(message, null);
    }


    default void replyPrivate(final String message, @Nullable final Consumer<Message> onSuccess, final Consumer<Throwable> onFail) {
        RestActions.sendPrivateMessage(getInvoker(), message, onSuccess, onFail);
    }

    default void replyImage(final String url) {
        replyImage(url, null);
    }

    default void replyImage(final String url, @Nullable final String message) {
        reply(RestActions.getMessageBuilder()
                .setEmbed(embedImage(url))
                .append(message != null ? message : "")
                .build()
        );
    }

    default void sendTyping() {
        RestActions.sendTyping(getChannel());
    }

    //name or nickname of the invoker
    @CheckReturnValue
    default String getEffectiveName() {
        return getMember()
                .map(Member::getEffectiveName)
                .orElse(getInvoker().getName());
    }

    @CheckReturnValue
    //nickname of the member entity of the provided user id in this guild or their user name or a placeholder
    default String getEffectiveName(final long userId) {
        return getGuild()
                .flatMap(guild -> Optional.ofNullable(guild.getMemberById(userId)))
                .map(Member::getEffectiveName)
                //fallback to global user lookup
                .or(() -> Launcher.getBotContext().getDiscordEntityProvider().getUserById(userId)
                        .map(User::getName))
                //fallback to placeholder
                .orElse(DiscordUser.UNKNOWN_NAME); //todo db lookup
    }

    //checks whether we have the provided permissions for the provided channel
    @CheckReturnValue
    static boolean hasPermissions(final TextChannel tc, final Permission... permissions) {
        return tc.getGuild().getSelfMember().hasPermission(tc, permissions);
    }

    Color BLACKIA = new Color(0, 24, 48); //blueish black that reminds of a clear nights sky

    /**
     * @return a general purpose preformatted builder for embeds
     */
    static EmbedBuilder getDefaultEmbedBuilder() {
        return RestActions.getEmbedBuilder()
                .setColor(BLACKIA)
                ;
    }


    // ********************************************************************************
    //                         Internal context stuff
    // ********************************************************************************

    private static MessageEmbed embedImage(final String url) {
        return getDefaultEmbedBuilder()
                .setImage(url)
                .build();
    }

    private void reply0(final Message message, @Nullable final Consumer<Message> onSuccess) {
        RestActions.sendMessage(getChannel(), message, onSuccess);
    }
}
