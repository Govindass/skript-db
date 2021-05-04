package com.btk5h.skriptdb.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Literal;
import ch.njol.skript.lang.SkriptEvent;
import ch.njol.skript.lang.SkriptParser;
import com.btk5h.skriptdb.events.SQLQueryCompleteEvent;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

public class EvtSQLQueryComplete extends SkriptEvent {
    static {
        Skript.registerEvent("complete of sql query", EvtSQLQueryComplete.class, SQLQueryCompleteEvent.class, "complete of [(sql|database)] query");
    }
        @Override
        public boolean init(final Literal<?>[] literals, final int i, final SkriptParser.ParseResult parseResult) {
            return true;
        }

        @Override
        public boolean check(Event event) {
            return (event instanceof SQLQueryCompleteEvent);
        }

    @Override
    public String toString(@Nullable Event event, boolean debug) {
        return "complete of sql query";
    }
}
