package com.btk5h.skriptdb.skript;

import com.btk5h.skriptdb.SkriptDB;
import com.btk5h.skriptdb.SkriptUtil;
import com.zaxxer.hikari.HikariDataSource;

import org.bukkit.Bukkit;
import org.bukkit.event.Event;
import org.eclipse.jdt.annotation.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.sql.rowset.CachedRowSet;

import ch.njol.skript.Skript;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.Expression;
import ch.njol.skript.lang.SkriptParser;
import ch.njol.skript.lang.TriggerItem;
import ch.njol.skript.lang.Variable;
import ch.njol.skript.lang.VariableString;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;

/**
 * Executes a statement on a database and optionally stores the result in a variable. Expressions
 * embedded in the query will be escaped to avoid SQL injection.
 * <p>
 * If a single variable, such as `{test}`, is passed, the variable will be set to the number of
 * affected rows.
 * <p>
 * If a list variable, such as `{test::*}`, is passed, the query result will be mapped to the list
 * variable in the form `{test::<column name>::<row number>}`
 *
 * Specifying `synchronously` will make skript-db execute the query on the event thread, which is useful for async
 * events. Note that skript-db will ignore this flag if you attempt to run this on the main thread.
 *
 * @name Execute Statement
 * @pattern [synchronously] execute %string% (in|on) %datasource% [and store [[the] (output|result)[s]] (to|in)
 * [the] [var[iable]] %-objects%]
 * @example execute "select * from table" in {sql} and store the result in {output::*}
 * @example execute "select * where player=%{player}%" in {sql} and store the result in {output::*}
 * @since 0.1.0
 */
public class EffSyncExecuteStatement extends Delay {
  static {
    Skript.registerEffect(EffSyncExecuteStatement.class, new String[] { "sync[hronous[ly]] execute %string% (in|on) %datasource% [and store [[the] (output|result)[s]] (to|in) [the] [var[iable]] %-objects%]" });
  }

  static String lastError;

  private static final ExecutorService threadPool =
      Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());

  private Expression<String> query;
  private Expression<HikariDataSource> dataSource;
  private VariableString var;
  private boolean isLocal;
  private boolean isList;
  private boolean isSyncOk;

  private void continueScriptExecution(Event e, String res) {
    lastError = res;

    if (getNext() != null) {
      TriggerItem.walk(getNext(), e);
    }
  }

  @Override
  protected void execute(Event e) {
    boolean isMainThread = Bukkit.isPrimaryThread();

    if (isSyncOk) {
      String result = syncExecuteStatement(e);
      continueScriptExecution(e, result);
    } else {

      CompletableFuture<String> sql =
          CompletableFuture.supplyAsync(() -> syncExecuteStatement(e), threadPool);

      sql.whenComplete((res, err) -> {
        if (err != null) {
          err.printStackTrace();
        }

        Bukkit.getScheduler().runTask(SkriptDB.getInstance(), () -> continueScriptExecution(e, res));
      });
    }
  }

  @Override
  protected TriggerItem walk(Event e) {
    debug(e, true);
	if (!isSyncOk) {
      SkriptUtil.delay(e);
	}
    execute(e);
    return null;
  }

  private String syncExecuteStatement(Event e) {
    HikariDataSource ds = dataSource.getSingle(e);

    if (ds == null) {
      return "Data source is not set";
    }

    try (Connection conn = ds.getConnection();
         PreparedStatement stmt = createStatement(e, conn)) {

      boolean hasResultSet = stmt.execute();

      if (var != null) {
        String baseVariable = var.toString(e)
            .toLowerCase(Locale.ENGLISH);
        if (isList) {
          baseVariable = baseVariable.substring(0, baseVariable.length() - 1);
        }

        if (hasResultSet) {
          CachedRowSet crs = SkriptDB.getRowSetFactory().createCachedRowSet();
          crs.populate(stmt.getResultSet());

          if (isList) {
            populateVariable(e, crs, baseVariable);
          } else {
            crs.last();
            setVariable(e, baseVariable, crs.getRow());
          }
        } else if (!isList) {
          setVariable(e, baseVariable, stmt.getUpdateCount());
        }
      }
    } catch (SQLException ex) {
      return ex.getMessage();
    }
    return null;
  }

  private PreparedStatement createStatement(Event e, Connection conn) throws SQLException {
    if (!(query instanceof VariableString)) {
      return conn.prepareStatement(query.getSingle(e));
    }

    if (((VariableString) query).isSimple()) {
      return conn.prepareStatement(SkriptUtil.getSimpleString(((VariableString) query)));
    }

    StringBuilder sb = new StringBuilder();
    List<Object> parameters = new ArrayList<>();
    Object[] objects = SkriptUtil.getTemplateString(((VariableString) query));
    for (int i = 0; i < objects.length; i++) {
      Object o = objects[i];
      if (o instanceof String) {
        sb.append(o);
      } else {
        Expression<?> expr = SkriptUtil.getExpressionFromInfo(o);

        String before = getString(objects, i - 1);
        String after = getString(objects, i + 1);
        boolean standaloneString = false;

        if (before != null && after != null) {
          if (before.endsWith("'") && after.endsWith("'")) {
            standaloneString = true;
          }
        }

        Object expressionValue = expr.getSingle(e);

        if (expr instanceof ExprUnsafe) {
          sb.append(expressionValue);

          if (standaloneString && expressionValue instanceof String) {
            String rawExpression = ((ExprUnsafe) expr).getRawExpression();
            Skript.warning(
                String.format("Unsafe may have been used unnecessarily. Try replacing 'unsafe %1$s' with %1$s",
                    rawExpression));
          }
        } else {
          parameters.add(expressionValue);
          sb.append('?');

          if (standaloneString) {
            Skript.warning("Do not surround expressions with quotes!");
          }
        }
      }
    }

    PreparedStatement stmt = conn.prepareStatement(sb.toString());

    for (int i = 0; i < parameters.size(); i++) {
      stmt.setObject(i + 1, parameters.get(i));
    }

    return stmt;
  }

  private String getString(Object[] objects, int index) {
    if (index < 0 || index >= objects.length) {
      return null;
    }

    Object object = objects[index];

    if (object instanceof String) {
      return (String) object;
    }

    return null;
  }

  private void setVariable(Event e, String name, Object obj) {
    Variables.setVariable(name.toLowerCase(Locale.ENGLISH), obj, e, isLocal);
  }

  private void populateVariable(Event e, CachedRowSet crs, String baseVariable)
      throws SQLException {
    ResultSetMetaData meta = crs.getMetaData();
    int columnCount = meta.getColumnCount();

    for (int i = 1; i <= columnCount; i++) {
      String label = meta.getColumnLabel(i);
      setVariable(e, baseVariable + label, label);
    }

    int rowNumber = 1;
    while (crs.next()) {
      for (int i = 1; i <= columnCount; i++) {
        setVariable(e, baseVariable + meta.getColumnLabel(i).toLowerCase(Locale.ENGLISH)
            + Variable.SEPARATOR + rowNumber, crs.getObject(i));
      }
      rowNumber++;
    }
  }

  @Override
  public String toString(@Nullable Event e, boolean debug) {
    return "execute " + query.toString(e, debug) + " in " + dataSource.toString(e, debug);
  }

  @SuppressWarnings("unchecked")
  @Override
  public boolean init(Expression<?>[] exprs, int matchedPattern, Kleenean isDelayed,
                      SkriptParser.ParseResult parseResult) {
    Expression<String> statementExpr = (Expression<String>) exprs[0];
    if (statementExpr instanceof VariableString || statementExpr instanceof ExprUnsafe) {
      query = statementExpr;
    } else {
      Skript.error("Database statements must be string literals. If you must use an expression, " +
          "you may use \"%unsafe (your expression)%\", but keep in mind, you may be vulnerable " +
          "to SQL injection attacks!");
      return false;
    }
    dataSource = (Expression<HikariDataSource>) exprs[1];
    Expression<?> expr = exprs[2];
    isSyncOk = true;
    if (expr instanceof Variable) {
      Variable<?> varExpr = (Variable<?>) expr;
      var = SkriptUtil.getVariableName(varExpr);
      isLocal = varExpr.isLocal();
      isList = varExpr.isList();
    } else if (expr != null) {
      Skript.error(expr + " is not a variable");
      return false;
    }
    return true;
  }
}
