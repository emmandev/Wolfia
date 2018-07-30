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

package space.npstr.wolfia.game;

import org.springframework.stereotype.Component;
import space.npstr.wolfia.db.Database;
import space.npstr.wolfia.db.entities.PrivateGuild;

import javax.annotation.Nullable;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * Created by napster on 11.05.18.
 * <p>
 * Manage available private guilds
 */
@Component
public class AvailablePrivateGuildQueue {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(AvailablePrivateGuildQueue.class);

    private final LinkedBlockingQueue<PrivateGuild> queue = new LinkedBlockingQueue<>();

    //todo make this a proper database based service?
    public AvailablePrivateGuildQueue(final Database database) {
        this.queue.addAll(database.getWrapper().selectJpqlQuery("FROM PrivateGuild", null, PrivateGuild.class));
        log.info("{} private guilds loaded", this.queue.size());
    }

    public List<PrivateGuild> getAll() {
        return Arrays.asList(queue.toArray(new PrivateGuild[0]));
    }

    //returns a free private guild immediately, or null.
    @Nullable
    public PrivateGuild poll() {
        return queue.poll();
    }

    //this is a blocking operation, and will always a free private guild
    public PrivateGuild take() throws InterruptedException {
        return queue.take();
    }

    public void free(final PrivateGuild privateGuild) {
        if (queue.contains(privateGuild)) {
            log.warn("Private guild #{} is already marked as free", privateGuild.getNumber());
            return;
        }
        queue.add(privateGuild);
    }
}
