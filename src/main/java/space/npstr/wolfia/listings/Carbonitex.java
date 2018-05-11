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

import javax.annotation.Nonnull;

/**
 * Created by napster on 17.11.17.
 */
public class Carbonitex extends Listing {

    public static final String NAME = "carbonitex.net";
    private final ListingsConfig listingsConfig;

    //https://www.carbonitex.net/
    //api docs: https://www.carbonitex.net/discord/data/botdata.php?key=MAH_KEY
    public Carbonitex(@Nonnull final OkHttpClient httpClient, final ListingsConfig listingsConfig,
                      final WolfiaConfig wolfiaConfig, final DiscordRequester discordRequester) {
        super(NAME, httpClient, wolfiaConfig, discordRequester);
        this.listingsConfig = listingsConfig;
    }

    @Override
    protected String createGlobalPayload(final ShardManager shardManager) {
        return new JSONObject()
                .put("key", this.listingsConfig.getCarbonitexKey())
                .put("servercount", shardManager.getGuildCache().size())
                .toString();
    }

    @Override
    protected boolean supportsShardPayload() {
        return false;
    }

    @Override
    protected String createShardPayload(@Nonnull final JDA jda) {
        throw new UnsupportedOperationException(NAME + " does not support shard payloads.");
    }

    @Nonnull
    @Override
    protected Request.Builder createRequest(final long botId, @Nonnull final String payload) {
        final RequestBody body = RequestBody.create(JSON, payload);
        return new Request.Builder()
                .url("https://www.carbonitex.net/discord/data/botdata.php")
                .addHeader("user-agent", getUserAgent())
                .post(body);
    }

    @Override
    protected boolean isConfigured() {
        final String carbonitexKey = this.listingsConfig.getCarbonitexKey();
        return carbonitexKey != null && !carbonitexKey.isEmpty();
    }
}
