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

import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.TextChannel;
import space.npstr.annotations.FieldsAreNonNullByDefault;
import space.npstr.annotations.ParametersAreNonnullByDefault;
import space.npstr.annotations.ReturnTypesAreNonNullByDefault;

import java.util.Optional;

/**
 * Created by napster on 09.12.17.
 * <p>
 * Provides @Nonnull methods for accessing guild entities after an elegant transformation from a CommandContext
 * Same rules as for the CommandContext, don't save these or hold on to these for an extended period of time as it
 * holds direct references to the entities.
 */
@FieldsAreNonNullByDefault
@ParametersAreNonnullByDefault
@ReturnTypesAreNonNullByDefault
public class GuildCommandContext extends CommandContext {

    public final Guild guild;
    public final Member member;
    public final TextChannel textChannel;


    @Override
    public Optional<Guild> getGuild() {
        return Optional.of(this.guild);
    }

    @Override
    public Optional<Member> getMember() {
        return Optional.of(this.member);
    }

    public Guild fetchGuild() {
        return this.guild;
    }

    public Member fetchMember() {
        return this.member;
    }

    public TextChannel getTextChannel() {
        return this.textChannel;
    }

    public GuildCommandContext(final CommandContext context, final Guild guild, final Member member,
                               final TextChannel textChannel) {
        super(context.event, context.trigger, context.args, context.rawArgs, context.command);
        this.guild = guild;
        this.member = member;
        this.textChannel = textChannel;
    }
}
