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

package space.npstr.wolfia.commands.debug;

import lombok.extern.slf4j.Slf4j;
import space.npstr.wolfia.Launcher;
import space.npstr.wolfia.Wolfia;
import space.npstr.wolfia.commands.BaseCommand;
import space.npstr.wolfia.commands.CommandContext;
import space.npstr.wolfia.commands.IOwnerRestricted;
import space.npstr.wolfia.utils.discord.Emojis;
import space.npstr.wolfia.utils.discord.TextchatUtils;
import space.npstr.wolfia.utils.log.DiscordLogger;

import javax.annotation.Nonnull;

/**
 * Created by napster on 21.06.17.
 */
@Slf4j
public class ShutdownCommand extends BaseCommand implements IOwnerRestricted {

    public static final int EXIT_CODE_SHUTDOWN = 0;
    //this is treated as an error code basically, so as long as whatever tool we are using to run the bot restarts it on
    // failure, this works just fine:tm:
    public static final int EXIT_CODE_RESTART = 2;

    private static boolean shutdownQueued = false;

    public ShutdownCommand(final String trigger, final String... aliases) {
        super(trigger, aliases);

        //this is possible cause the JVM runs shutdown hooks concurrently so we dont have to fear that this is run after
        // the main shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> shutdownQueued = true, "set-shutdown-flag"));
    }

    @Nonnull
    @Override
    public String help() {
        return "Shutdown or restart the bot.";
    }

    @Override
    public synchronized boolean execute(@Nonnull final CommandContext context) {
        final int runningGamesCount = Launcher.getBotContext().getGameRegistry().getRunningGamesCount();
        if (isShutdownQueued()) {
            context.replyWithName(String.format("shutdown has been queued already! **%s** games still running.",
                    runningGamesCount));
            return false;
        }

        final boolean isRestart = context.trigger.equalsIgnoreCase("restart");
        final int exitCode = isRestart ? EXIT_CODE_RESTART : EXIT_CODE_SHUTDOWN;

        final String message = String.format("queueing %s! **%s** games are still running.",
                isRestart ? "restart" : "shutdown", runningGamesCount);
        context.replyWithMention(message, __ -> {
            final Thread t = new Thread(() -> shutdown(exitCode), "shutdown-thread");
            t.setUncaughtExceptionHandler(Wolfia.uncaughtExceptionHandler);
            t.start();
        });

        return true;
    }

    public static boolean isShutdownQueued() {
        return shutdownQueued;
    }

    private static void shutdown(final int code) {
        DiscordLogger.getLogger().log("%s `%s` Shutting down with exit code %s",
                Emojis.DOOR, TextchatUtils.berlinTime(), code);
        log.info("Exiting with code {}", code);
        System.exit(code);
    }

}
