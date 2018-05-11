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

package space.npstr.wolfia.discord;

import com.google.common.base.Suppliers;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.entities.User;
import org.springframework.aop.framework.Advised;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.function.Supplier;

/**
 * Created by napster on 11.05.18.
 */
@Slf4j
@Component
public class DiscordEntityProvider {

    private final Supplier<ShardManager> lazyShardManager;

    public DiscordEntityProvider(@Lazy final ShardManager shardManagerProxy) {

        //unwrap the spring proxy of the lazy bean
        // we require the raw bean, because the proxy will error out when accessed during shutdown hooks, but
        // we manage the lifecycle of the shardManager singleton ourselves, so we don't need spring refusing us to serve
        // a perfectly fine bean during shutdown hooks.
        this.lazyShardManager = Suppliers.memoize(() -> {
            try {
                final ShardManager target = (ShardManager) ((Advised) shardManagerProxy).getTargetSource().getTarget();
                if (target == null) {
                    throw new IllegalStateException();
                }
                return target;
            } catch (final Exception e) {
                log.error("Failed to unproxy the shard manager", e);
                //this should not happen but if it does, just work with the proxy. however we might error out during
                // execution of shutdown handlers that rely on fetching jdaentities
                return shardManagerProxy;
            }
        });
    }

    public ShardManager getShardManager() {
        return this.lazyShardManager.get();
    }

    public Optional<User> getUserById(final long userId) {
        return Optional.ofNullable(getShardManager().getUserById(userId));
    }

    public Optional<Guild> getGuildById(final long guildId) {
        return Optional.ofNullable(getShardManager().getGuildById(guildId));
    }

    public Optional<TextChannel> getTextChannelById(final long textChannelId) {
        return Optional.ofNullable(getShardManager().getTextChannelById(textChannelId));
    }

    public Optional<JDA> anyShard() {
        return getShardManager().getShardCache().stream().findAny();
    }

    public User getSelf() {
        return anyShard().orElseThrow().getSelfUser();  //todo this can be improved by fetching the user from discord
    }

    public boolean allShardsUp() {
        final ShardManager shardManager = getShardManager();
        if (shardManager.getShards().size() < shardManager.getShardsTotal()) {
            return false;
        }
        for (final JDA jda : shardManager.getShards()) {
            if (jda.getStatus() != JDA.Status.CONNECTED) {
                return false;
            }
        }
        return true;
    }
}
