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

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import space.npstr.sqlsauce.jda.listeners.GuildCachingListener;
import space.npstr.sqlsauce.jda.listeners.UserMemberCachingListener;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.db.entities.CachedGuild;
import space.npstr.wolfia.db.entities.CachedUser;

/**
 * Created by napster on 13.05.18.
 */
@Configuration
public class JdaListenersConfiguration {

    @Bean
    public GuildCachingListener<CachedGuild> guildGuildCachingListener(final Database database) {
        return new GuildCachingListener<>(database.getWrapper(), CachedGuild.class);
    }

    @Bean
    public UserMemberCachingListener<CachedUser> userMemberCachingListener(final Database database) {
        return new UserMemberCachingListener<>(database.getWrapper(), CachedUser.class);
    }

}
