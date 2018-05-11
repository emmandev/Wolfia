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

package space.npstr.wolfia;

import ch.qos.logback.classic.LoggerContext;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDAInfo;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.flyway.FlywayAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceTransactionManagerAutoConfiguration;
import org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration;
import org.springframework.boot.context.event.ApplicationFailedEvent;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.game.definitions.Games;
import space.npstr.wolfia.game.tools.Scheduler;
import space.npstr.wolfia.utils.GitRepoState;
import space.npstr.wolfia.utils.discord.TextchatUtils;
import space.npstr.wolfia.utils.log.DiscordLogger;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Created by napster on 10.05.18.
 */
@Slf4j
@SpringBootApplication
@EnableAutoConfiguration(exclude = { //we handle these ourselves
        DataSourceAutoConfiguration.class,
        DataSourceTransactionManagerAutoConfiguration.class,
        HibernateJpaAutoConfiguration.class,
        FlywayAutoConfiguration.class
})
public class Launcher implements ApplicationRunner {

    @SuppressWarnings("NullableProblems")
    private static BotContext botContext;

    private final Thread shutdownHook;

    public static BotContext getBotContext() {
        return botContext;
    }

    public static void main(final String[] args) {
        //just post the info to the console
        if (args.length > 0 &&
                (args[0].equalsIgnoreCase("-v")
                        || args[0].equalsIgnoreCase("--version")
                        || args[0].equalsIgnoreCase("-version"))) {
            System.out.println("Version flag detected. Printing version info, then exiting.");
            System.out.println(getVersionInfo());
            System.out.println("Version info printed, exiting.");
            return;
        }

        log.info(getVersionInfo());

        System.setProperty("spring.config.name", "wolfia");
        final SpringApplication app = new SpringApplication(Launcher.class);
        app.addListeners(event -> {
            if (event instanceof ApplicationFailedEvent) {
                final ApplicationFailedEvent failed = (ApplicationFailedEvent) event;
                log.error("Application failed", failed.getException());
            }
        });
        app.run(args);
    }

    public Launcher(final BotContext botContext, final Scheduler scheduler, ShardManager shardManager, Database database) {
        Launcher.botContext = botContext;

        shutdownHook = new Thread(() -> {
            log.info("Shutdown hook triggered! {} games still ongoing.", Games.getRunningGamesCount());
            final Future waitForGamesToEnd = scheduler.getScheduler().submit(() -> {
                while (Games.getRunningGamesCount() > 0) {
                    log.info("Waiting on {} games to finish.", Games.getRunningGamesCount());
                    try {
                        Thread.sleep(10000);
                    } catch (final InterruptedException ignored) {
                    }
                }
            });
            try {
                //is this value is changed, make sure to adjust the one in docker-update.sh
                waitForGamesToEnd.get(2, TimeUnit.HOURS); //should be enough until the forseeable future
                //todo persist games (big changes)
            } catch (final ExecutionException | InterruptedException | TimeoutException e) {
                log.error("dafuq", e);
            }
            if (Games.getRunningGamesCount() > 0) {
                log.warn("Killing {} games while exiting", Games.getRunningGamesCount());
            }

            log.info("Shutting down discord logger");
            DiscordLogger.shutdown(10, TimeUnit.SECONDS);

            //okHttpClient claims that a shutdown isn't necessary

            //shutdown JDA
            log.info("Shutting down shards");
            shardManager.shutdown();

            //shutdown executors
            log.info("Shutting down executor");
            final List<Runnable> runnablesEx = Wolfia.executor.shutdownNow();
            log.info("{} runnables canceled", runnablesEx.size());
            try {
                Wolfia.executor.awaitTermination(30, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while awaiting executor termination");
            }

            log.info("Shutting down scheduler");
            final List<Runnable> runnablesSc = scheduler.getScheduler().shutdownNow();
            log.info("{} runnables canceled", runnablesSc.size());
            try {
                scheduler.getScheduler().awaitTermination(30, TimeUnit.SECONDS);
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                log.warn("Interrupted while awaiting scheduler termination");
            }

            //shutdown DB
            log.info("Shutting down database");
            database.shutdown();

            //shutdown logback logger
            log.info("Shutting down logger :rip:");
            final LoggerContext loggerContext = (LoggerContext) LoggerFactory.getILoggerFactory();
            loggerContext.stop();
        }, "shutdown-hook");
    }

    @Override
    public void run(final ApplicationArguments args) throws Exception {

        Runtime.getRuntime().addShutdownHook(shutdownHook);

        Wolfia.main(args.getSourceArgs());
    }


    @Nonnull
    private static String getVersionInfo() {
        return art
                + "\n"
                + "\n\tVersion:       " + App.VERSION
                + "\n\tBuild:         " + App.BUILD_NUMBER
                + "\n\tBuild time:    " + TextchatUtils.toBerlinTime(App.BUILD_TIME)
                + "\n\tCommit:        " + GitRepoState.getGitRepositoryState().commitIdAbbrev + " (" + GitRepoState.getGitRepositoryState().branch + ")"
                + "\n\tCommit time:   " + TextchatUtils.toBerlinTime(GitRepoState.getGitRepositoryState().commitTime * 1000)
                + "\n\tJVM:           " + System.getProperty("java.version")
                + "\n\tJDA:           " + JDAInfo.VERSION
                + "\n";
    }

    //########## vanity
    private static final String art = "\n"
            + "\n                              __"
            + "\n                            .d$$b"
            + "\n                           .' TO$;\\"
            + "\n        Wolfia            /  : TP._;"
            + "\n    Werewolf & Mafia     / _.;  :Tb|"
            + "\n      Discord bot       /   /   ;j$j"
            + "\n                    _.-\"       d$$$$"
            + "\n                  .' ..       d$$$$;"
            + "\n                 /  /P'      d$$$$P. |\\"
            + "\n                /   \"      .d$$$P' |\\^\"l"
            + "\n              .'           `T$P^\"\"\"\"\"  :"
            + "\n          ._.'      _.'                ;"
            + "\n       `-.-\".-'-' ._.       _.-\"    .-\""
            + "\n     `.-\" _____  ._              .-\""
            + "\n    -(.g$$$$$$$b.              .'"
            + "\n      \"\"^^T$$$P^)            .(:"
            + "\n        _/  -\"  /.'         /:/;"
            + "\n     ._.'-'`-'  \")/         /;/;"
            + "\n  `-.-\"..--\"\"   \" /         /  ;"
            + "\n .-\" ..--\"\"        -'          :"
            + "\n ..--\"\"--.-\"         (\\      .-(\\"
            + "\n   ..--\"\"              `-\\(\\/;`"
            + "\n     _.                      :"
            + "\n                             ;`-"
            + "\n                            :\\"
            + "\n                            ;";
}
