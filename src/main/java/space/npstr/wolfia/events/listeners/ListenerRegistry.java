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

package space.npstr.wolfia.events.listeners;

import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.hooks.EventListener;
import org.springframework.stereotype.Component;
import space.npstr.sqlsauce.jda.listeners.GuildCachingListener;
import space.npstr.sqlsauce.jda.listeners.UserMemberCachingListener;
import space.npstr.wolfia.events.CommandListener;
import space.npstr.wolfia.events.InternalListener;
import space.npstr.wolfia.events.WolfiaGuildListener;
import space.npstr.wolfia.game.AvailablePrivateGuildQueue;

/**
 * Created by napster on 13.05.18.
 */
@Component
public class ListenerRegistry {


    private final ShardManager shardManager;

    public ListenerRegistry(final ShardManager shardManager, final AvailablePrivateGuildQueue availablePrivateGuildQueue,
                            final CommandListener commandListener, final InternalListener internalListener,
                            final WolfiaGuildListener wolfiaGuildListener, final GuildCachingListener guildCachingListener,
                            final UserMemberCachingListener userMemberCachingListener) {
        this.shardManager = shardManager;
        this.shardManager.addEventListener(availablePrivateGuildQueue.getAll().toArray());
        this.shardManager.addEventListener(commandListener);
        this.shardManager.addEventListener(internalListener);
        this.shardManager.addEventListener(wolfiaGuildListener);
        this.shardManager.addEventListener(guildCachingListener);
        this.shardManager.addEventListener(userMemberCachingListener);
    }


    public void addListener(final EventListener eventListener) {
        shardManager.addEventListener(eventListener);
    }

    public void removeListener(final EventListener eventListener) {
        shardManager.removeEventListener(eventListener);
    }
}
