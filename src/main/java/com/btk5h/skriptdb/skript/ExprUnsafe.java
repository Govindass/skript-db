package com.btk5h.skriptdb.skript;

import org.bukkit.event.Event;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.ExpressionType;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.util.SimpleExpression;
import ch.njol.util.Kleenean;

/**
 * Opts out of automatic SQL injection protection for a specific expression in a statement.
 *
 * @name Unsafe Expression
 * @pattern unsafe %text%
 * @return text
 * @example execute "select %unsafe {columns variable}% from %{table variable}%" in {sql}
 * @example execute unsafe {fully dynamic query} in {sql}
 * @since 0.1.0
 */
public class ExprUnsafe extends SimpleExpression<String> {
  static {
    Skript.registerExpression(ExprUnsafe.class, String.class, ExpressionType.COMBINED,
        "unsafe %string%");
  }

  private Expression<String> stringExpression;
  private String rawExpression;

  public String getRawExpression() {
    return rawExpression;
  }

  @Override
  protected String[] get(Event e) {
    return stringExpression.getArray(e);
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
    return "unsafe " + stringExpression.toString(e, debug);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    stringExpression = (Expression<String>) exprs[0];
    rawExpression = parseResult.expr.substring("unsafe".length()).trim();
    return true;
  }
}
