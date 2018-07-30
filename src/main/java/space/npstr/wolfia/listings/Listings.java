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

package space.npstr.wolfia.listings;

import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.events.ReadyEvent;
import net.dv8tion.jda.core.events.guild.GuildJoinEvent;
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import okhttp3.OkHttpClient;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.config.properties.ListingsConfig;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.discord.DiscordRequester;
import space.npstr.wolfia.game.tools.Scheduler;

import javax.annotation.Nullable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * Created by napster on 23.07.17.
 * <p>
 * Takes care of posting all our stats to various listing sites
 */
@Component
public class Listings extends ListenerAdapter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Listings.class);

    //serves as both a set of registered listings and keeping track of ongoing tasks of posting stats
    private final Map<Listing, Future> tasks = new HashMap<>();

    private final Scheduler scheduler;

    public Listings(final OkHttpClient.Builder defaultHttpClientBuilder, final ListingsConfig listingsConfig,
                    final WolfiaConfig wolfiaConfig, final Scheduler scheduler, final ShardManager shardManager,
                    final DiscordRequester discordRequester) {
        this.scheduler = scheduler;
        final OkHttpClient listingsHttpClient = defaultHttpClientBuilder
//                .eventListener(new Metrics.HttpEventListener("listings")) todo add metrics
                .build();

        final DiscordBotsPw botsPw = new DiscordBotsPw(listingsHttpClient, listingsConfig, wolfiaConfig, discordRequester);
        this.tasks.put(botsPw, null);
        final DiscordBotsOrg botsOrg = new DiscordBotsOrg(listingsHttpClient, listingsConfig, wolfiaConfig, discordRequester);
        this.tasks.put(botsOrg, null);
        final Carbonitex carbonitex = new Carbonitex(listingsHttpClient, listingsConfig, wolfiaConfig, discordRequester);
        this.tasks.put(carbonitex, null);

        final int initialDelay = 0;
        final int period = 5;
        final TimeUnit timeUnit = TimeUnit.MINUTES;
        scheduler.scheduleExceptionSafeAtFixedRate(() -> botsPw.postGlobalStats(shardManager),
                "Failed to post global listing stats to " + DiscordBotsPw.NAME,
                initialDelay, period, timeUnit);
        scheduler.scheduleExceptionSafeAtFixedRate(() -> botsOrg.postGlobalStats(shardManager),
                "Failed to post global listing stats to " + DiscordBotsOrg.NAME,
                initialDelay, period, timeUnit);
        scheduler.scheduleExceptionSafeAtFixedRate(() -> carbonitex.postGlobalStats(shardManager),
                "Failed to post global listing stats to " + Carbonitex.NAME,
                initialDelay, period, timeUnit);
    }

    private static boolean isTaskRunning(@Nullable final Future task) {
        return task != null && !task.isDone() && !task.isCancelled();
    }

    //according to discordbotspw and discordbotsorg docs: post stats on guild join, guild leave, and ready events
    private void postAllStats(final JDA jda) {
        final Set<Listing> listings = new HashSet<>(this.tasks.keySet());
        for (final Listing listing : listings) {
            postStats(listing, jda);
        }
    }

    private synchronized void postStats(final Listing listing, final JDA jda) {
        final Future task = this.tasks.get(listing);
        if (isTaskRunning(task)) {
            log.info("Skipping posting stats to {} since there is a task to do that running already.", listing.name);
            return;
        }

        tasks.put(listing, scheduler.getScheduler().submit(() -> {
            try {
                if (listing.supportsShardPayload()) {
                    listing.postShardStats(jda);
                }
            } catch (final InterruptedException e) {
                log.error("Task to send stats to {} interrupted", listing.name, e);
            }
        }));
    }


    @Override
    public void onGuildJoin(final GuildJoinEvent event) {
        postAllStats(event.getJDA());
    }

    @Override
    public void onGuildLeave(final GuildLeaveEvent event) {
        postAllStats(event.getJDA());
    }

    @Override
    public void onReady(final ReadyEvent event) {
        postAllStats(event.getJDA());
    }
}
