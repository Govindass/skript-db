package com.btk5h.skriptdb.skript;

import ch.njol.skript.Skript;
import ch.njol.skript.effects.Delay;
import ch.njol.skript.lang.*;
import ch.njol.skript.variables.Variables;
import ch.njol.util.Kleenean;
import ch.njol.util.Pair;
import com.btk5h.skriptdb.SkriptDB;
import com.btk5h.skriptdb.SkriptUtil;
import com.zaxxer.hikari.HikariDataSource;
import org.bukkit.Bukkit;
import org.bukkit.event.Event;

import javax.sql.DataSource;
import javax.sql.rowset.CachedRowSet;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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
 * @name Execute Statement
 * @pattern [synchronously] execute %string% (in|on) %datasource% [and store [[the] (output|result)[s]] (to|in)
 * [the] [var[iable]] %-objects%]
 * @example execute "select * from table" in {sql} and store the result in {output::*}
 * @example execute "select * from %{table variable}%" in {sql} and store the result in {output::*}
 * @since 0.1.0
 */
public class EffExecuteStatement extends Effect {
    private static final ExecutorService threadPool =
            Executors.newCachedThreadPool();
    static String lastError;

    static {
        Skript.registerEffect(EffExecuteStatement.class,
                "execute %string% (in|on) %datasource% " +
                        "[and store [[the] (output|result)[s]] (to|in) [the] [var[iable]] %-objects%]", "quickly execute %string% (in|on) %datasource% " +
                        "[and store [[the] (output|result)[s]] (to|in) [the] [var[iable]] %-objects%]");
    }

    private Expression<String> query;
    private Expression<HikariDataSource> dataSource;
    private VariableString var;
    private boolean isLocal;
    private boolean isList;
    private boolean isSync;

    private void continueScriptExecution(Event e, Object populatedVariables) {
        lastError = null;
        if (populatedVariables instanceof String) {
            lastError = (String) populatedVariables;
        } else {

            if (getNext() != null) {
                ((Map<String, Object>) populatedVariables).forEach((name, value) -> setVariable(e, name, value));
            }
        }
        TriggerItem.walk(getNext(), e);
    }

    @Override
    protected void execute(Event e) {
        DataSource ds = dataSource.getSingle(e);
        Pair<String, List<Object>> query = parseQuery(e);
        String baseVariable = var != null ? var.toString(e).toLowerCase(Locale.ENGLISH) : null;
        //if data source isn't set
        if (ds == null) return;

        boolean sync = !Bukkit.isPrimaryThread();

        //if current thread is not main thread, then make this query to not have delays

        Object locals = Variables.removeLocals(e);

        //execute SQL statement
        CompletableFuture<Object> sql =
                CompletableFuture.supplyAsync(() -> executeStatement(ds, baseVariable, query), threadPool);

        //when SQL statement is completed
        boolean finalSync = sync;
        sql.whenComplete((res, err) -> {
            if (err != null) {
                err.printStackTrace();
            }

            //handle last error syntax data
            lastError = null;
            if (res instanceof String) {
                lastError = (String) res;
            }

            if (getNext() != null) {
                //if local variables are present
                if (locals != null)
                    //bring back local variables

                    //populate SQL data into variables
                    if (!(res instanceof String)) {

                        //also set variables in the sql query complete event

                        //TEMPORARILY DISABLED, AS THIS WOULD WORSEN PERFORMANCE OF THE QUERIES AND NOT BE USED BY MOST PEOPLE.
                        //I may add config option to enable this later?

                        //SQLQueryCompleteEvent event = new SQLQueryCompleteEvent(this.query.getSingle(e));
                        //((Map<String, Object>) res).forEach((name, value) -> setVariable(event, name, value));
                        //SkriptDB.getPlugin(SkriptDB.class).getServer().getPluginManager().callEvent(event);
                    }
                if (isSync || finalSync) {

                    Variables.setLocalVariables(e, locals);
                    if (!(res instanceof String)) {
                        ((Map<String, Object>) res).forEach((name, value) -> setVariable(e, name, value));
                    }
                    TriggerItem.walk(getNext(), e);
                    Variables.removeLocals(e);
                } else {
                    Bukkit.getScheduler().runTask(SkriptDB.getInstance(), () -> {
                        Variables.setLocalVariables(e, locals);
                        if (!(res instanceof String)) {
                            ((Map<String, Object>) res).forEach((name, value) -> setVariable(e, name, value));
                        }
                        TriggerItem.walk(getNext(), e);
                        //the line below is required to prevent memory leaks
                        //no functionality difference notice with it being removed from my test, but the memory gets filled with leaks
                        //so it must be kept
                        Variables.removeLocals(e);
                    });
                }
            }
        });
    }

    @Override
    protected TriggerItem walk(Event e) {
        debug(e, true);
        //I think no longer needed as of 1.3.0, uncomment if something breaks
        if (!isSync) {
            Delay.addDelayedEvent(e);
        }
        execute(e);
        return null;
    }

    private Pair<String, List<Object>> parseQuery(Event e) {
        if (!(query instanceof VariableString)) {
            return new Pair<>(query.getSingle(e), null);
        }
        VariableString q = (VariableString) query;
        if (q.isSimple()) {
            return new Pair<>(q.toString(e), null);
        }

        StringBuilder sb = new StringBuilder();
        List<Object> parameters = new ArrayList<>();
        Object[] objects = SkriptUtil.getTemplateString(q);

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
        return new Pair<>(sb.toString(), parameters);
    }

    private Object executeStatement(DataSource ds, String baseVariable, Pair<String, List<Object>> query) {
        if (ds == null) {
            return "Data source is not set";
        }
        Map<String, Object> variableList = new HashMap<>();
        try (Connection conn = ds.getConnection();
             PreparedStatement stmt = createStatement(conn, query)) {

            boolean hasResultSet = stmt.execute();

            if (baseVariable != null) {
                if (isList) {
                    baseVariable = baseVariable.substring(0, baseVariable.length() - 1);
                }

                if (hasResultSet) {
                    CachedRowSet crs = SkriptDB.getRowSetFactory().createCachedRowSet();
                    crs.populate(stmt.getResultSet());

                    if (isList) {
                        ResultSetMetaData meta = crs.getMetaData();
                        int columnCount = meta.getColumnCount();

                        for (int i = 1; i <= columnCount; i++) {
                            String label = meta.getColumnLabel(i);
                            variableList.put(baseVariable + label, label);
                        }

                        int rowNumber = 1;
                        try {
                            while (crs.next()) {
                                for (int i = 1; i <= columnCount; i++) {
                                    variableList.put(baseVariable + meta.getColumnLabel(i).toLowerCase(Locale.ENGLISH)
                                            + Variable.SEPARATOR + rowNumber, crs.getObject(i));
                                }
                                rowNumber++;
                            }
                        } catch (SQLException ex) {
                            return ex.getMessage();
                        }
                    } else {
                        crs.last();
                        variableList.put(baseVariable, crs.getRow());
                    }
                } else if (!isList) {
                    //if no results are returned and the specified variable isn't a list variable, put the affected rows count in the variable
                    variableList.put(baseVariable, stmt.getUpdateCount());
                }
            }
        } catch (SQLException ex) {
            return ex.getMessage();
        }
        return variableList;
    }

    private PreparedStatement createStatement(Connection conn, Pair<String, List<Object>> query) throws SQLException {
        PreparedStatement stmt = conn.prepareStatement(query.getFirst());
        List<Object> parameters = query.getSecond();

        if (parameters != null) {
            for (int i = 0; i < parameters.size(); i++) {
                stmt.setObject(i + 1, parameters.get(i));
            }
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

    @Override
    public String toString(Event e, boolean debug) {
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
        isSync = matchedPattern == 1;
        if (expr instanceof Variable) {
            Variable<?> varExpr = (Variable<?>) expr;
            var = varExpr.getName();
            isLocal = varExpr.isLocal();
            isList = varExpr.isList();
        } else if (expr != null) {
            Skript.error(expr + " is not a variable");
            return false;
        }
        return true;
    }
}
