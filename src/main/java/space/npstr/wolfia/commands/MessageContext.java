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

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.MessageChannel;
import net.dv8tion.jda.core.entities.User;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import space.npstr.annotations.FieldsAreNonNullByDefault;
import space.npstr.annotations.ParametersAreNonnullByDefault;
import space.npstr.annotations.ReturnTypesAreNonNullByDefault;
import space.npstr.wolfia.App;

import javax.annotation.CheckReturnValue;
import java.util.Optional;

/**
 * Created by napster on 16.11.17.
 * <p>
 * Contexts intended for fast usage, dont save these in any kind of variables
 */
@FieldsAreNonNullByDefault
@ParametersAreNonnullByDefault
@ReturnTypesAreNonNullByDefault
public class MessageContext implements Context {

    public final MessageChannel channel;
    public final User invoker;
    public final Message msg;
    public final MessageReceivedEvent event;
    public final JDA jda;


    public MessageContext(final MessageReceivedEvent event) {
        this.channel = event.getChannel();
        this.invoker = event.getAuthor();
        this.msg = event.getMessage();
        this.event = event;
        this.jda = event.getJDA();
    }


    @Override
    @CheckReturnValue
    public MessageChannel getChannel() {
        return this.channel;
    }

    @Override
    @CheckReturnValue
    public User getInvoker() {
        return this.invoker;
    }

    @Override
    @CheckReturnValue
    public Message getMessage() {
        return this.msg;
    }

    @Override
    @CheckReturnValue
    public Optional<Guild> getGuild() {
        return Optional.ofNullable(this.event.getGuild());
    }

    @Override
    @CheckReturnValue
    public Optional<Member> getMember() {
        return Optional.ofNullable(this.event.getMember());
    }

    /**
     * @return true if the invoker is the bot owner, false otherwise
     */
    public boolean isOwner() {
        return App.isOwner(this.invoker);
    }
}
