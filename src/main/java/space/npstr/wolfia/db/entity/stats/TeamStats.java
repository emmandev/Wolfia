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

package space.npstr.wolfia.db.entity.stats;

import space.npstr.wolfia.game.definitions.Alignments;

import javax.persistence.*;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by napster on 01.06.17.
 * <p>
 * model of a team in a game
 */
@Entity
@Table(name = "stats_team")
public class TeamStats implements Serializable {

    private static final long serialVersionUID = 7168610781984059851L;

    //dont really care about this one, its for the database
    @Id
    @GeneratedValue
    @Column(name = "team_id")
    private long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_id")
    private GameStats game;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "team", orphanRemoval = true)
    @Column(name = "players")
    private Set<PlayerStats> players = new HashSet<>();

    //defined in the Alignments enum
    @Column(name = "alignment")
    private String alignment;

    //watch out: teams of the same alignment (example: wolves) may not have the same name, or the equals function will
    //go haywire
    @Column(name = "name")
    private String name;

    @Column(name = "is_winner")
    private boolean isWinner;


    public TeamStats(final GameStats game, final Alignments alignment, final String name) {
        this.game = game;
        this.alignment = alignment.name();
        this.name = name;
        this.isWinner = false;
    }

    public void addPlayer(final PlayerStats player) {
        this.players.add(player);
    }


    //do not use the autogenerated id, it will only be set after persisting
    @Override
    public int hashCode() {
        final int prime = 31;
        //todo test if referencing game here produces any kind of errors, since it's marked as lazy loading
        int result = this.game.hashCode();
        result = prime * result + this.alignment.hashCode();
        result = prime * result + this.name.hashCode();
        return result;
    }

    //do not compare the autogenerated id, it will only be set after persisting
    @Override
    public boolean equals(final Object obj) {
        if (!(obj instanceof TeamStats)) {
            return false;
        }
        final TeamStats t = (TeamStats) obj;
        //todo test if referencing game here produces any kind of errors, since it's marked as lazy loading
        return this.game.equals(t.game) && this.alignment.equals(t.alignment) && this.name.equals(t.name);
    }


    //########## boilerplate code below


    TeamStats() {
    }

    public long getId() {
        return this.id;
    }

    public void setId(final long id) {
        this.id = id;
    }

    public GameStats getGame() {
        return this.game;
    }

    public void setGame(final GameStats game) {
        this.game = game;
    }

    public Set<PlayerStats> getPlayers() {
        return this.players;
    }

    public void setPlayers(final Set<PlayerStats> players) {
        this.players = players;
    }

    public Alignments getAlignment() {
        return Alignments.valueOf(this.alignment);
    }

    public void setAlignment(final Alignments alignment) {
        this.alignment = alignment.name();
    }

    public String getName() {
        return this.name;
    }

    public void setName(final String name) {
        this.name = name;
    }

    public boolean isWinner() {
        return this.isWinner;
    }

    public void setWinner(final boolean winner) {
        this.isWinner = winner;
    }
}
