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

package space.npstr.wolfia.game.tools;

import org.springframework.stereotype.Component;
import space.npstr.wolfia.Wolfia;

import java.util.concurrent.Executor;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by napster on 11.05.18.
 */
@Component
public class Scheduler {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(Scheduler.class);

    private final ScheduledThreadPoolExecutor scheduler;

    public Scheduler() {
        this.scheduler = new ExceptionLoggingExecutor(100, "scheduler");
        this.scheduler.setRemoveOnCancelPolicy(true);
        final AtomicInteger threadCounter = new AtomicInteger();
        this.scheduler.setThreadFactory(r -> {
            final Thread t = new Thread(r, "scheduler-t" + threadCounter.getAndIncrement());
            t.setUncaughtExceptionHandler(Wolfia.uncaughtExceptionHandler);
            return t;
        });

//        threadPoolCollector.addPool("scheduler", scheduler); todo add metrics
    }

    public Executor getExecutor() {
        return this.scheduler;
    }

    public ScheduledThreadPoolExecutor getScheduler() {
        return this.scheduler;
    }

    @SuppressWarnings("UnusedReturnValue")
    public Future scheduleExceptionSafeAtFixedRate(final ExceptionLoggingExecutor.ExceptionalRunnable task, final String errorLogMessage,
                                                   final int initialDelay, final int period, final TimeUnit timeUnit) {
        return this.scheduler.scheduleAtFixedRate(() -> {
            try {
                task.run();
            } catch (final Exception e) {
                log.error(errorLogMessage, e);
            }
        }, initialDelay, period, timeUnit);
    }

}
