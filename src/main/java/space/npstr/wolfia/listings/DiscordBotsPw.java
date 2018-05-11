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
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import org.json.JSONObject;
import space.npstr.wolfia.config.properties.ListingsConfig;
import space.npstr.wolfia.config.properties.WolfiaConfig;
import space.npstr.wolfia.discord.DiscordRequester;

/**
 * Created by napster on 06.10.17.
 */
public class DiscordBotsPw extends Listing {

    public static final String NAME = "bots.discord.pw";
    private final ListingsConfig listingsConfig;

    //https://bots.discord.pw
    //api docs: https://bots.discord.pw/api
    public DiscordBotsPw(final OkHttpClient httpClient, final ListingsConfig listingsConfig,
                         final WolfiaConfig wolfiaConfig, final DiscordRequester discordRequester) {
        super(NAME, httpClient, wolfiaConfig, discordRequester);
        this.listingsConfig = listingsConfig;
    }

    @Override
    protected String createGlobalPayload(final ShardManager shardManager) {
        return new JSONObject()
                .put("server_count", shardManager.getGuildCache().size())
                .toString();
    }

    @Override
    protected String createShardPayload(final JDA jda) {
        return new JSONObject()
                .put("shard_id", jda.getShardInfo().getShardId())
                .put("shard_count", jda.getShardInfo().getShardTotal())
                .put("server_count", jda.getGuildCache().size())
                .toString();
    }

    @Override
    protected Request.Builder createRequest(final long botId, final String payload) {
        final RequestBody body = RequestBody.create(JSON, payload);
        return new Request.Builder()
                .url(String.format("https://bots.discord.pw/api/bots/%s/stats", botId))
                .addHeader("user-agent", getUserAgent())
                .addHeader("Authorization", this.listingsConfig.getBotsPwToken())
                .post(body);
    }

    @Override
    protected boolean isConfigured() {
        final String botsPwToken = this.listingsConfig.getBotsPwToken();
        return botsPwToken != null && !botsPwToken.isEmpty();
    }
}
