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

import net.dv8tion.jda.bot.sharding.DefaultShardManagerBuilder;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.entities.Game;
import net.dv8tion.jda.core.utils.cache.CacheFlag;
import okhttp3.OkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import space.npstr.wolfia.App;
import space.npstr.wolfia.config.properties.WolfiaConfig;

import javax.security.auth.login.LoginException;
import java.util.EnumSet;

/**
 * Created by napster on 11.05.18.
 */
@Configuration
public class ShardManagerConfiguration {


    @Bean(destroyMethod = "") //we manage the lifecycle ourselves tyvm, see shutdown hook in the launcher / wolfia class
    public ShardManager shardManager(final WolfiaConfig wolfiaConfig, final OkHttpClient.Builder httpClientBuilder)
            throws LoginException {
        return new DefaultShardManagerBuilder()
                .setToken(wolfiaConfig.getDiscordToken())
                .setGame(Game.playing(App.GAME_STATUS))
                .setHttpClientBuilder(httpClientBuilder)
                .setDisabledCacheFlags(EnumSet.of(CacheFlag.GAME, CacheFlag.VOICE_STATE))
                .setEnableShutdownHook(false)
                .setAudioEnabled(false)
                .build();
    }

}
