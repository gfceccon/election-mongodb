package database;

import com.mongodb.BasicDBObject;

import java.sql.*;
import java.util.ArrayList;
import java.util.Map;
import java.util.Stack;


public class Oracle
{
    private Connection connection;
    private Stack<Statement> stmt;
    private Stack<ResultSet> rs;


    private Oracle()
    {
    }

    public static Oracle connect(String user, String password) throws ClassNotFoundException, SQLException
    {
        Oracle database = new Oracle();
        Class.forName("oracle.jdbc.driver.OracleDriver");
        database.connection = DriverManager.getConnection(
                "jdbc:oracle:thin:@grad.icmc.usp.br:15215:orcl",
                user,
                password);
        database.stmt = new Stack<>();
        database.rs = new Stack<>();
        return database;
    }

    public void close() throws SQLException
    {
        connection.close();
    }


    public ArrayList<SQLTable> getTables() throws SQLException
    {
        String query;
        SQLTable.allTables.clear();
        ArrayList<SQLTable> tables = new ArrayList<>();
        SQLTable.TableType type = SQLTable.TableType.USER;

        query = type.getQuery();

        stmt.push(connection.createStatement());
        rs.push(stmt.peek().executeQuery(query));

        while (rs.peek().next())
        {
            SQLTable table = new SQLTable(rs.peek().getString("NAME").toUpperCase(), type);
            tables.add(table);
            SQLTable.allTables.put(table.name, table);
        }
        stmt.pop().close();

        for (SQLTable table : tables)
            fillTable(table);

        return tables;
    }

    private void fillTable(SQLTable table) throws SQLException
    {
        String query;
        query = String.format("SELECT COLUMN_NAME AS NAME, DATA_TYPE AS TYPE, DATA_LENGTH AS LENGTH FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '%s'", table.name);

        stmt.push(connection.createStatement());
        rs.push(stmt.peek().executeQuery(query));

        while (rs.peek().next())
        {
            SQLTableColumn column = new SQLTableColumn();
            column.name = rs.peek().getString("NAME").toUpperCase();
            column.type = rs.peek().getString("TYPE").toUpperCase();
            table.allColumns.put(column.name, column);
        }

        stmt.pop().close();

        query = String.format("SELECT TNAME, CNAME, CTYPE, CONSTRAINT, CONDITION, COLS.TABLE_NAME AS RTNAME, COLS.COLUMN_NAME AS RCNAME FROM (SELECT COLS.TABLE_NAME AS TNAME, COLS.COLUMN_NAME AS CNAME, CONS.CONSTRAINT_TYPE AS CTYPE, CONS.CONSTRAINT_NAME AS CONSTRAINT, CONS.SEARCH_CONDITION AS CONDITION, CONS.R_CONSTRAINT_NAME, COLS.POSITION FROM USER_CONSTRAINTS CONS JOIN USER_CONS_COLUMNS COLS ON CONS.CONSTRAINT_NAME = COLS.CONSTRAINT_NAME WHERE CONS.CONSTRAINT_TYPE IN ('P', 'R', 'U') AND COLS.TABLE_NAME = '%s') TCONS LEFT JOIN USER_CONS_COLUMNS COLS ON COLS.CONSTRAINT_NAME = TCONS.R_CONSTRAINT_NAME AND TCONS.POSITION = COLS.POSITION", table.name);

        stmt.push(connection.createStatement());
        rs.push(stmt.peek().executeQuery(query));

        while (rs.peek().next())
        {
            String columnName = rs.peek().getString("CNAME").toUpperCase();
            String type = rs.peek().getString("CTYPE").toUpperCase();
            SQLTableColumn column = table.allColumns.get(columnName);


            if (type.equals("P"))
            {
                table.primaryKeys.add(column);
                column.isPrimary = true;
            }

            if (type.equals("R"))
            {
                column.isForeign = true;
                String refName = rs.peek().getString("CONSTRAINT").toUpperCase();
                String refTable = rs.peek().getString("RTNAME").toUpperCase();
                String refColumn = rs.peek().getString("RCNAME").toUpperCase();
                SQLTableReference ref = new SQLTableReference(refName, SQLTable.allTables.get(refTable), refColumn);
                column.references.add(ref);

                if(!table.foreignKeys.contains(column))
                    table.foreignKeys.add(column);
                table.foreignKeysTables.add(ref);
            }

            if(type.equals("U"))
            {
                table.uniques.add(column);
            }
        }

        stmt.pop().close();

        for (SQLTableColumn column : table.allColumns.values())
        {
            if (!column.isPrimary && !column.isForeign)
                table.columns.add(column);
        }
    }

    public void begin(SQLTable table) throws SQLException
    {
        stmt.push(connection.createStatement());
        rs.push(stmt.peek().executeQuery(String.format("SELECT * FROM %s", table.name)));
    }

    public void begin(SQLTable table, BasicDBObject values) throws SQLException
    {
        stmt.push(connection.createStatement());
        StringBuilder query = new StringBuilder();
        query.append(String.format("SELECT * FROM %s WHERE ", table.name));
        Map map = values.toMap();
        map.forEach((key, value) -> query.append(key).append(" = '").append(value).append("'").append(" AND "));
        query.append("1=1");
        rs.push(stmt.peek().executeQuery(query.toString()));
    }

    public ResultSet next() throws SQLException
    {
        if(rs.peek().next())
            return rs.peek();
        else
            return null;
    }

    public void end() throws SQLException
    {
        rs.pop();
        stmt.pop().close();
    }
}
