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

import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.bot.sharding.ShardManager;
import net.dv8tion.jda.core.JDA;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import space.npstr.wolfia.App;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.discord.DiscordRequester;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by napster on 06.10.17.
 * <p>
 * Template for various bot listing sites
 */
@Slf4j
public abstract class Listing {

    protected static final MediaType JSON = parseMediaType("application/json; charset=utf-8");

    protected final String name;
    private final OkHttpClient httpClient;
    private final WolfiaConfig wolfiaConfig;
    private final DiscordRequester discordRequester;

    //id to paylod. -1 for global
    private final Map<Integer, String> lastPayloads = new HashMap<>();

    public Listing(final String name, final OkHttpClient httpClient, final WolfiaConfig wolfiaConfig,
                   final DiscordRequester discordRequester) {
        this.name = name;
        this.httpClient = httpClient;
        this.wolfiaConfig = wolfiaConfig;
        this.discordRequester = discordRequester;
    }

    private static MediaType parseMediaType(final String input) {
        final MediaType mediaType = MediaType.parse(input);
        if (mediaType == null) {
            throw new IllegalArgumentException("Not a mediatype: " + input);
        }
        return mediaType;
    }

    protected abstract String createGlobalPayload(ShardManager shardManager);

    //override to false for list sites that do not support shard payloads (like carbonitex for example)
    protected boolean supportsShardPayload() {
        return true;
    }

    protected abstract String createShardPayload(JDA jda);

    protected abstract Request.Builder createRequest(long botId, String payload);

    //return false if there is no token configured, or whatever is needed to post to the site
    protected abstract boolean isConfigured();

    protected String getUserAgent() {
        return "DiscordBot (" + this.discordRequester.getAppInfo().getName() + ", "
                + App.GITHUB_LINK + ", "
                + App.VERSION + ")";
    }

    public void postGlobalStats(final ShardManager shardManager) throws InterruptedException {
        if (!isConfigured()) {
            log.debug("Skipping posting global stats to {} due to not being configured", this.name);
            return;
        }
        if (this.wolfiaConfig.isDebug()) {
            log.info("Skipping posting global stats to {} due to running in debug mode", this.name);
            return;
        }

        for (final JDA jda : shardManager.getShards()) {
            if (jda.getStatus() != JDA.Status.CONNECTED) {
                log.info("Skipping posting global stats to {} due to not all shards being up", this.name);
                return;
            }
        }
        sendRequestUntilSuccess(-1, createGlobalPayload(shardManager));
    }

    //retries with growing delay until it is successful
    public void postShardStats(final JDA jda) throws InterruptedException {
        if (!isConfigured()) {
            log.debug("Skipping posting shard stats to {} due to not being configured", this.name);
            return;
        }
        if (this.wolfiaConfig.isDebug()) {
            log.info("Skipping posting shard stats to {} due to running in debug mode", this.name);
            return;
        }
        sendRequestUntilSuccess(jda.getShardInfo().getShardId(), createShardPayload(jda));
    }

    //id may be -1 for global stats
    private void sendRequestUntilSuccess(final int id, final String payload) throws InterruptedException {
        if (payload.equals(this.lastPayloads.get(id))) {
            log.info("Skipping sending stats of shard {} to {} since the payload has not changed", id, this.name);
            return;
        }

        final Request req = createRequest(this.discordRequester.getAppInfo().getId(), payload).build();

        int attempt = 0;
        boolean success = false;
        while (!success) {
            attempt++;
            try (final Response response = this.httpClient.newCall(req).execute()) {
                if (response.isSuccessful()) {
                    log.info("Successfully posted bot stats of shard {} to {} on attempt {}, code {}", id, this.name, attempt, response.code());
                    this.lastPayloads.put(id, payload);
                    success = true;
                } else {
                    //noinspection ConstantConditions
                    log.info("Failed to post stats of shard {} to {} on attempt {}: code {}, body:\n{}",
                            id, this.name, attempt, response.code(), response.body().string());
                }
            } catch (final IOException e) {
                log.info("Failed to post stats of shard {} to {} on attempt {}", id, this.name, attempt, e);
            }

            if (!success) {
                //increase delay with growing attempts to avoid overloading the listing servers
                Thread.sleep(attempt * 10000L); //10 sec
            }

            if (attempt == 10 || attempt == 100) { // no need to spam these
                log.warn("Attempt {} to post stats of shard {} to {} unsuccessful. See logs for details.", id, attempt, this.name);
            }
        }
    }

    @Override
    public boolean equals(final Object obj) {
        return obj instanceof Listing && this.name.equals(((Listing) obj).name);
    }
}