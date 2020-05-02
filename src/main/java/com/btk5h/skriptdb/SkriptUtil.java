package com.btk5h.skriptdb;

import ch.njol.skript.Skript;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.VariableString;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Optional;

public class SkriptUtil {

  private static final Field STRING;
  private static final Field EXPR;

  static {
    Field _FIELD = null;
    try {
      _FIELD = VariableString.class.getDeclaredField("string");
      _FIELD.setAccessible(true);
    } catch (NoSuchFieldException e) {
      Skript.error("Skript's 'string' field could not be resolved.");
      e.printStackTrace();
    }
    STRING = _FIELD;

    try {
      Optional<Class<?>> expressionInfo = Arrays.stream(VariableString.class.getDeclaredClasses())
          .filter(cls -> cls.getSimpleName().equals("ExpressionInfo"))
          .findFirst();
      if (expressionInfo.isPresent()) {
        Class<?> expressionInfoClass = expressionInfo.get();
        _FIELD = expressionInfoClass.getDeclaredField("expr");
        _FIELD.setAccessible(true);
      } else {
        Skript.error("Skript's 'ExpressionInfo' class could not be resolved.");
      }
    } catch (NoSuchFieldException e) {
      e.printStackTrace();
      Skript.error("Skript's 'expr' field could not be resolved.");
    }
    EXPR = _FIELD;
  }

  public static Object[] getTemplateString(VariableString vs) {
    try {
      return (Object[]) STRING.get(vs);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

  public static Expression<?> getExpressionFromInfo(Object o) {
    try {
      return (Expression<?>) EXPR.get(o);
    } catch (IllegalAccessException e) {
      throw new RuntimeException(e);
    }
  }

}
