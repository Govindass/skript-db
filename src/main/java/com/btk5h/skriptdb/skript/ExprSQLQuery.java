package com.btk5h.skriptdb.skript;

import ch.njol.skript.ScriptLoader;
import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.skript.log.ErrorQuality;
import ch.njol.util.Kleenean;
import com.btk5h.skriptdb.events.SQLQueryCompleteEvent;
import org.bukkit.event.Event;

/**
 * Stores the error from the last executed statement, if there was one.
 *
 * @name Last Data Source Error
 * @pattern [the] [last] (sql|db|data(base|[ ]source)) error
 * @return text
 * @since 0.1.0
 */
public class ExprSQLQuery extends SimpleExpression<String> {
    static {
        Skript.registerExpression(ExprSQLQuery.class, String.class,
                ExpressionType.SIMPLE, "sql query");
    }


    @Override
    protected String[] get(Event e) {
        if (e instanceof SQLQueryCompleteEvent) {
            return new String[]{((SQLQueryCompleteEvent) e).getQuery()};
        }
        return null;
    }

    @Override
    public boolean isSingle() {
        return true;
    }

    @Override
    public Class<? extends String> getReturnType() {
        return String.class;
    }

    @Override
    public String toString(Event e, boolean debug) {
        return "sql query";
    }

    @Override
    public boolean init(final Expression<?>[] expressions, final int matchedPattern, final Kleenean isDelayed, final SkriptParser.ParseResult parseResult) {
        if (!ScriptLoader.isCurrentEvent(SQLQueryCompleteEvent.class)) {
            Skript.error("Cannot use 'sql query' outside of a complete of sql query event", ErrorQuality.SEMANTIC_ERROR);
            return false;
        }
        return true;
    }
}
