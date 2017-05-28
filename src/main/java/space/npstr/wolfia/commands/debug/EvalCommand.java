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

import net.dv8tion.jda.core.JDA;
import net.dv8tion.jda.core.entities.Guild;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.Message;
import net.dv8tion.jda.core.entities.TextChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import space.npstr.wolfia.commands.meta.CommandParser;
import space.npstr.wolfia.commands.meta.ICommand;
import space.npstr.wolfia.commands.meta.IOwnerRestricted;
import space.npstr.wolfia.game.Games;
import space.npstr.wolfia.game.Setups;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.concurrent.*;

/**
 * Created by napster on 27.05.17.
 * <p>
 * run js code in the bot
 */
public class EvalCommand implements ICommand, IOwnerRestricted {

    public static final String COMMAND = "eval";
    private static final Logger log = LoggerFactory.getLogger(EvalCommand.class);

    //Thanks Fred & Dinos!
    private final ScriptEngine engine;

    public EvalCommand() {
        this.engine = new ScriptEngineManager().getEngineByName("nashorn");
        try {
            this.engine.eval("var imports = new JavaImporter(java.io, java.lang, java.util);");

        } catch (final ScriptException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void execute(final CommandParser.CommandContainer commandInfo) {
        final Guild guild = commandInfo.event.getGuild();
        final TextChannel channel = commandInfo.event.getTextChannel();
        final Message message = commandInfo.event.getMessage();
        final Member author = commandInfo.event.getMember();

        final JDA jda = guild.getJDA();

        channel.sendTyping().queue();

        final String source = commandInfo.beheaded.substring(commandInfo.command.length()).trim();

        this.engine.put("jda", jda);
        this.engine.put("api", jda);
        this.engine.put("channel", channel);
        this.engine.put("author", author);
        this.engine.put("bot", jda.getSelfUser());
        this.engine.put("member", guild.getSelfMember());
        this.engine.put("message", message);
        this.engine.put("guild", guild);
        this.engine.put("game", Games.get(channel.getIdLong()));
        this.engine.put("setup", Setups.get(channel.getIdLong()));

        final ScheduledExecutorService service = Executors.newScheduledThreadPool(1);
        final ScheduledFuture<?> future = service.schedule(() -> {

            final Object out;
            try {
                out = this.engine.eval(
                        "(function() {"
                                + "with (imports) {\n" + source + "\n}"
                                + "})();");

            } catch (final Exception ex) {
                channel.sendMessage("`" + ex.getMessage() + "`").queue();
                log.error("Error occurred in eval", ex);
                return;
            }

            final String outputS;
            if (out == null) {
                outputS = ":ok_hand::skin-tone-3:";
            } else if (out.toString().contains("\n")) {
                outputS = "\nEvalCommand: ```\n" + out.toString() + "```";
            } else {
                outputS = "\nEvalCommand: `" + out.toString() + "`";
            }

            channel.sendMessage("```java\n" + source + "```" + "\n" + outputS).queue();

        }, 0, TimeUnit.MILLISECONDS);

        final Thread script = new Thread("EvalCommand") {
            @Override
            public void run() {
                try {
                    future.get(10, TimeUnit.SECONDS);

                } catch (final TimeoutException ex) {
                    future.cancel(true);
                    channel.sendMessage("Task exceeded time limit.").queue();
                } catch (final Exception ex) {
                    channel.sendMessage("`" + ex.getMessage() + "`").queue();
                }
            }
        };
        script.start();
    }

    @Override
    public String help() {
        return "Run js eval code on the bot";
    }
}
