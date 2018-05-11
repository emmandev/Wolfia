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
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.TextChannel;
import net.dv8tion.jda.core.hooks.EventListener;
import space.npstr.wolfia.commands.debug.SyncCommand;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.game.tools.ExceptionLoggingExecutor;
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

    public static final long START_TIME = System.currentTimeMillis();
    //todo find a better way to execute tasks; java's built in ScheduledExecutorService is rather crappy for many reasons; until then a big-sized pool size will suffice to make sure tasks get executed when they are due
    public static final ExceptionLoggingExecutor executor = new ExceptionLoggingExecutor(100, "main-scheduled-executor");

    public static final Thread.UncaughtExceptionHandler uncaughtExceptionHandler
            = (t, e) -> log.error("Uncaught exception in thread {}", t.getName(), e);

    private static boolean started = false;

    //set up things that are crucial
    //if something fails exit right away
    public static void main(final String[] args) throws InterruptedException {
        final WolfiaConfig wolfiaConfig = Launcher.getBotContext().getWolfiaConfig();

        if (wolfiaConfig.isDebug())
            log.info("Running DEBUG configuration");
        else
            log.info("Running PRODUCTION configuration");

        //try connecting in a reasonable timeframe
        boolean dbConnected = false;
        final long dbConnectStarted = System.currentTimeMillis();
        do {
            try {
                //noinspection ResultOfMethodCallIgnored
                Launcher.getBotContext().getDatabase().getWrapper().selectSqlQuery("SELECT 1;", null);
                dbConnected = true;
                log.info("Initial db connection succeeded");
            } catch (final Exception e) {
                log.info("Failed initial db connection, retrying in a moment", e);
                Thread.sleep(1000);
            }
        } while (!dbConnected && System.currentTimeMillis() - dbConnectStarted < 1000 * 60 * 2); //2 minutes

        if (!dbConnected) {
            log.error("Failed to init db connection in a reasonable amount of time, exiting.");
            System.exit(2);
        }

        //wait for all shards to be online, then start doing things that expect the full bot to be online
        while (!allShardsUp()) {
            Thread.sleep(1000);
        }
        started = true;

        //sync guild cache
        // this takes a few seconds to do, so do it as the last thing of the main method, or put it into it's own thread
        SyncCommand.syncGuilds(executor, Launcher.getBotContext().getDiscordEntityProvider().getShardManager().getGuildCache().stream(), null);
        //user cache is not synced on each start as it takes a lot of time and resources. see SyncComm for manual triggering
    }

    private Wolfia() {
    }

    /**
     * @return true if wolfia has started and all systems are expected to be operational
     */
    public static boolean isStarted() {
        return started;
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

    public static void addEventListener(final EventListener eventListener) {
        Launcher.getBotContext().getDiscordEntityProvider().getShardManager().addEventListener(eventListener);
    }

    public static void removeEventListener(final EventListener eventListener) {
        Launcher.getBotContext().getDiscordEntityProvider().getShardManager().removeEventListener(eventListener);
    }

    public static boolean allShardsUp() {
        final ShardManager shardManager = Launcher.getBotContext().getDiscordEntityProvider().getShardManager();
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
