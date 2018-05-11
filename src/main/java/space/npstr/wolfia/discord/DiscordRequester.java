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

package space.npstr.wolfia.discord;

import com.google.common.base.Suppliers;
import net.dv8tion.jda.core.requests.Requester;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.stereotype.Component;
import space.npstr.wolfia.config.properties.WolfiaConfig;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Created by napster on 05.05.18.
 * <p>
 * Request REST data from discord without the need to be connected. Data is reasonably cached.
 */
@Component
public class DiscordRequester {

    private final WolfiaConfig wolfiaConfig;
    private final OkHttpClient defaultHttpClient;

    //memoizers
    private final Supplier<Integer> recommendedShardCountSupplier = Suppliers.memoize(this::fetchRecommendedShardCount);
    private final Supplier<DiscordAppInfo> discordAppInfoSupplier = Suppliers
            .memoizeWithExpiration(this::fetchApplicationInfo, 1, TimeUnit.HOURS);

    public DiscordRequester(final WolfiaConfig wolfiaConfig, final OkHttpClient defaultHttpClient) {
        this.wolfiaConfig = wolfiaConfig;
        this.defaultHttpClient = defaultHttpClient;
    }

    public int getRecommendedShardCount() {
        return this.recommendedShardCountSupplier.get();
    }

    public DiscordAppInfo getAppInfo() {
        return this.discordAppInfoSupplier.get();
    }

    private int fetchRecommendedShardCount() {
        final Request request = new Request.Builder()
                .url(Requester.DISCORD_API_PREFIX + "gateway/bot")
                .header("Authorization", "Bot " + this.wolfiaConfig.getDiscordToken())
                .header("user-agent", Requester.USER_AGENT)
                .build();
        final String rawBody;
        try (final Response response = this.defaultHttpClient.newCall(request).execute()) {
            //noinspection ConstantConditions
            rawBody = response.body().string();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to fetch recommended shards", e);
        }

        try {
            return new JSONObject(rawBody).getInt("shards");
        } catch (final JSONException e) {
            throw new RuntimeException("Failed to parse recommended shards: " + rawBody, e);
        }
    }

    private DiscordAppInfo fetchApplicationInfo() {
        final Request request = new Request.Builder()
                .url(Requester.DISCORD_API_PREFIX + "oauth2/applications/@me")
                .header("Authorization", "Bot " + this.wolfiaConfig.getDiscordToken())
                .header("user-agent", Requester.USER_AGENT)
                .build();

        final String rawBody;
        try (final Response response = this.defaultHttpClient.newCall(request).execute()) {
            //noinspection ConstantConditions
            rawBody = response.body().string();
        } catch (final IOException e) {
            throw new RuntimeException("Failed to fetch application info", e);
        }

        try {
            final JSONObject json = new JSONObject(rawBody);
            return new DiscordAppInfo(json);
        } catch (final JSONException e) {
            throw new RuntimeException("Failed to parse application info from " + rawBody, e);
        }
    }

}
