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

import net.dv8tion.jda.core.entities.impl.UserImpl;
import org.json.JSONObject;

import javax.annotation.Nullable;

/**
 * Created by napster on 05.05.18.
 *
 * @see space.npstr.wolfia.discord.DiscordRequester
 */
public class DiscordAppInfo {

    private final String description;
    private final boolean doesBotRequireCodeGrant;
    @Nullable
    private final String iconId;
    private final long id;
    private final String name;
    private final boolean isBotPublic;

    //owner info
    private final long ownerId;
    private final String ownerName;
    private final String ownerDiscrim;
    @Nullable
    private final String ownerAvatarId;


    public DiscordAppInfo(final JSONObject json) {
        this.description = json.getString("description");
        this.doesBotRequireCodeGrant = json.getBoolean("bot_require_code_grant");
        this.iconId = json.optString("icon", null);
        this.id = json.getLong("id");
        this.name = json.getString("name");
        this.isBotPublic = json.getBoolean("bot_public");

        final JSONObject owner = json.getJSONObject("owner");
        this.ownerId = owner.getLong("id");
        this.ownerName = owner.getString("username");
        this.ownerDiscrim = owner.get("discriminator").toString();
        this.ownerAvatarId = owner.optString("avatar", null);
    }


    public String getDescription() {
        return this.description;
    }

    public boolean isDoesBotRequireCodeGrant() {
        return this.doesBotRequireCodeGrant;
    }

    @Nullable
    public String getIconId() {
        return this.iconId;
    }

    // ty JDA
    @Nullable
    public String getIconUrl() {
        return getIconId() == null ? null
                : "https://cdn.discordapp.com/app-icons/" + getId() + '/' + getIconId() + ".png";
    }

    public long getId() {
        return this.id;
    }

    public String getName() {
        return this.name;
    }

    public boolean isBotPublic() {
        return this.isBotPublic;
    }

    public long getOwnerId() {
        return this.ownerId;
    }

    public String getOwnerName() {
        return this.ownerName;
    }

    public String getOwnerDiscrim() {
        return this.ownerDiscrim;
    }

    @Nullable
    public String getOwnerAvatarId() {
        return this.ownerAvatarId;
    }

    // ty JDA
    public String getOwnerDefaultAvatarId() {
        return UserImpl.DefaultAvatar.values()[Integer.parseInt(getOwnerDiscrim()) % UserImpl.DefaultAvatar.values().length].toString();
    }

    // ty JDA
    public String getOwnerDefaultAvatarUrl() {
        return "https://discordapp.com/assets/" + getOwnerDefaultAvatarId() + ".png";
    }

    // ty JDA
    public String getEffectiveOwnerAvatarUrl() {
        final String ownerAvatarId = getOwnerAvatarId();
        return ownerAvatarId == null ? getOwnerDefaultAvatarUrl() : "https://cdn.discordapp.com/avatars/" + getOwnerId() + "/" + ownerAvatarId
                + (ownerAvatarId.startsWith("a_") ? ".gif" : ".png");
    }
}
