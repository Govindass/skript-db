package com.btk5h.skriptdb.skript;

import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

/**
 * Stores the error from the last executed statement, if there was one.
 *
 * @name Last Data Source Error
 * @pattern [the] [last] (sql|db|data(base|[ ]source)) error
 * @return text
 * @since 0.1.0
 */
public class ExprDBError extends SimpleExpression<String> {
  static {
    Skript.registerExpression(ExprDBError.class, String.class,
        ExpressionType.SIMPLE, "[the] [last] (sql|db|data(base|[ ]source)) error");
  }

  @Override
  protected String[] get(Event e) {
    return new String[]{EffExecuteStatement.lastError};
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
    return "last database error";
  }

  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed, SkriptParser.ParseResult parseResult) {
    return true;
  }
}
