/*
 * Copyright (C) 2017-2018 Dennis Neufeld
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

package space.npstr.wolfia.game;

import net.dv8tion.jda.core.entities.TextChannel;
import org.springframework.stereotype.Component;
import space.npstr.annotations.FieldsAreNonNullByDefault;
import space.npstr.annotations.ParametersAreNonnullByDefault;
import space.npstr.annotations.ReturnTypesAreNonNullByDefault;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by napster on 11.05.18.
 */
@Component
@FieldsAreNonNullByDefault
@ParametersAreNonnullByDefault
@ReturnTypesAreNonNullByDefault
public class GameRegistry {

    private final Map<Long, Game> gameRegistry = new ConcurrentHashMap<>();


    public Map<Long, Game> getAll() {
        return new HashMap<>(gameRegistry);
    }

    /**
     * @return game that is running in the specified channel; may return null
     */
    public Optional<Game> get(final long channelId) {
        return Optional.of(gameRegistry.get(channelId));
    }

    public Optional<Game> get(@Nonnull final TextChannel channel) {
        return get(channel.getIdLong());
    }

    //useful for evaling
    public Game get(final String channelId) {
        return gameRegistry.get(Long.valueOf(channelId));
    }

    public void remove(final Game game) {
        gameRegistry.remove(game.getChannelId());
    }

    public void remove(final long channelId) {
        gameRegistry.remove(channelId);
    }

    public void set(final Game game) {
        gameRegistry.put(game.getChannelId(), game);
    }

    public int getRunningGamesCount() {
        return gameRegistry.size();
    }

}
