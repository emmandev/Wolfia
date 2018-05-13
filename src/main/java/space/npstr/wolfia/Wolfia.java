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

package space.npstr.wolfia;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.core.entities.TextChannel;
import space.npstr.wolfia.utils.log.LogTheStackException;

import java.util.Optional;

/**
 * Created by npstr on 22.08.2016
 * <p>
 * Main class of Wolfia
 * //general list of todos etc
 * //todo rename role pm/dm -> rolecard
 */
@Slf4j
public class Wolfia {

    public static final Thread.UncaughtExceptionHandler uncaughtExceptionHandler
            = (t, e) -> log.error("Uncaught exception in thread {}", t.getName(), e);

    private Wolfia() {
    }

    //this method assumes that the id itself is legit and not a mistake
    // it is an attempt to improve the occasional inconsistency of discord which makes looking up entities a gamble
    // the main feature being the @Nonnull return contract, over the @Nullable contract of looking the entity up in JDA
    //todo what happens if we leave a server? do we get stuck in here? maybe make this throw an exception eventually and exit?
    public static TextChannel fetchTextChannel(final long channelId) {
        Optional<TextChannel> tc;
        do {
            tc = Launcher.getBotContext().getDiscordEntityProvider().getTextChannelById(channelId);

            if (!tc.isPresent()) {
                log.error("Could not find channel {}, retrying in a moment", channelId, new LogTheStackException());
                try {
                    Thread.sleep(5000);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        } while (!tc.isPresent());

        return tc.get();
    }
}
