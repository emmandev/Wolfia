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

package space.npstr.wolfia.config;

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.Game;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import space.npstr.sqlsauce.jda.listeners.GuildCachingListener;
import space.npstr.sqlsauce.jda.listeners.UserMemberCachingListener;
import space.npstr.wolfia.App;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.db.entities.CachedGuild;
import space.npstr.wolfia.db.entities.CachedUser;
import space.npstr.wolfia.events.CommandListener;
import space.npstr.wolfia.events.InternalListener;
import space.npstr.wolfia.events.WolfiaGuildListener;
import space.npstr.wolfia.game.AvailablePrivateGuildQueue;

import javax.security.auth.login.LoginException;

/**
 * Created by napster on 11.05.18.
 */
@Slf4j
@Configuration
public class ShardManagerConfiguration {


    @Bean(destroyMethod = "") //we manage the lifecycle ourselves tyvm, see shutdown hook in the launcher / wolfia class
    public ShardManager shardManager(final WolfiaConfig wolfiaConfig, final OkHttpClient.Builder httpClientBuilder,
                                     final Database database, final CommandListener commandListener,
                                     final AvailablePrivateGuildQueue availablePrivateGuildQueue)
            throws LoginException {
        return new DefaultShardManagerBuilder()
                .setToken(wolfiaConfig.getDiscordToken())
                .setGame(Game.playing(App.GAME_STATUS))
                .addEventListeners(commandListener)
                .addEventListeners(availablePrivateGuildQueue.getAll().toArray())
                .addEventListeners(new UserMemberCachingListener<>(database.getWrapper(), CachedUser.class))
                .addEventListeners(new GuildCachingListener<>(database.getWrapper(), CachedGuild.class))
                .addEventListeners(new InternalListener())
                .addEventListeners(new WolfiaGuildListener())
                .setHttpClientBuilder(httpClientBuilder)
                .setEnableShutdownHook(false)
                .setAudioEnabled(false)
                .build();
    }

}
