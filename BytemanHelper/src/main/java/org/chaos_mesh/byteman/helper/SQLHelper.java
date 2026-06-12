package org.chaos_mesh.byteman.helper;

import java.util.*;

import org.jboss.byteman.rule.Rule;
import org.jboss.byteman.rule.helper.Helper;
import net.sf.jsqlparser.parser.*;
import net.sf.jsqlparser.*;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.replace.Replace;
import net.sf.jsqlparser.util.TablesNamesFinder;

public class SQLHelper extends Helper
{
    protected SQLHelper(Rule rule) {
        super(rule);
    }

    // extractSQL returns the SQL string from the direct parameter if non-null,
    // otherwise extracts it from the Query object via reflection.
    // This handles PreparedStatement paths where NativeSession.execSQL receives
    // null as the query string ($2) because the SQL is compiled into a packet.
    // Uses reflection to avoid compile-time dependency on MySQL Connector/J.
    public String extractSQL(Object callingQuery, String query) {
        if (query != null) {
            return query;
        }
        if (callingQuery == null) {
            return null;
        }
        try {
            // For PreparedStatements, callingQuery is ClientPreparedStatement.
            // Its ancestor StatementImpl declares `protected Query query` which
            // is a ClientPreparedQuery (implements PreparedQuery).
            // PreparedQuery.getOriginalSql() returns the SQL text.
            Class<?> cls = callingQuery.getClass();
            while (cls != null && cls != Object.class) {
                try {
                    java.lang.reflect.Field f = cls.getDeclaredField("query");
                    f.setAccessible(true);
                    Object innerQuery = f.get(callingQuery);
                    if (innerQuery != null) {
                        java.lang.reflect.Method m = innerQuery.getClass().getMethod("getOriginalSql");
                        String sql = (String) m.invoke(innerQuery);
                        if (sql != null) {
                            System.out.println("extractSQL: recovered SQL from PreparedQuery: " + sql);
                            return sql;
                        }
                    }
                } catch (NoSuchFieldException e) {
                    cls = cls.getSuperclass();
                    continue;
                }
                break;
            }
        } catch (Exception e) {
            System.out.println("extractSQL: reflection failed: " + e);
        }
        return null;
    }

    public boolean matchDBTable(String database, String sql, String filterDatabase, String filterTable, String sqlType) {
        return SQLParser.matchDBTable(database, sql, filterDatabase, filterTable, sqlType);
    }
}



