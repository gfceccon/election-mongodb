package database;

import java.sql.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.ArrayList;


public class Oracle
{
    private Connection connection;
    private Statement stmt;
    private ResultSet rs;


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
        stmt = connection.createStatement();
        rs = stmt.executeQuery(query);
        while (rs.next())
        {
            SQLTable table = new SQLTable(rs.getString("NAME"), type);
            tables.add(table);
            SQLTable.allTables.put(table.name, table);
        }
        stmt.close();

        for (SQLTable table : tables)
            fillTable(table);

        return tables;
    }

    private void fillTable(SQLTable table) throws SQLException
    {
        String query;
        query = String.format("SELECT COLUMN_NAME AS NAME, DATA_TYPE AS TYPE, DATA_LENGTH AS LENGTH FROM USER_TAB_COLUMNS WHERE TABLE_NAME = '%s'", table.name);
        stmt = connection.createStatement();
        rs = stmt.executeQuery(query);

        while (rs.next())
        {
            SQLTableColumn column = new SQLTableColumn();
            column.name = rs.getString("NAME");
            column.type = rs.getString("TYPE");
            table.allColumns.put(column.name, column);
        }

        stmt.close();

        query = String.format("SELECT TNAME, CNAME, CTYPE, CONDITION, COLS.TABLE_NAME AS RTNAME, COLS.COLUMN_NAME AS RCNAME FROM (SELECT COLS.TABLE_NAME AS TNAME, COLS.COLUMN_NAME AS CNAME, CONS.CONSTRAINT_TYPE AS CTYPE, CONS.SEARCH_CONDITION AS CONDITION, CONS.R_CONSTRAINT_NAME, COLS.POSITION FROM USER_CONSTRAINTS CONS JOIN USER_CONS_COLUMNS COLS ON CONS.CONSTRAINT_NAME = COLS.CONSTRAINT_NAME WHERE CONS.CONSTRAINT_TYPE IN ('P', 'R', 'U') AND COLS.TABLE_NAME = '%s') TCONS LEFT JOIN USER_CONS_COLUMNS COLS ON COLS.CONSTRAINT_NAME = TCONS.R_CONSTRAINT_NAME AND TCONS.POSITION = COLS.POSITION", table.name);
        stmt = connection.createStatement();
        rs = stmt.executeQuery(query);

        while (rs.next())
        {
            String columnName = rs.getString("CNAME");
            String type = rs.getString("CTYPE");
            SQLTableColumn column = table.allColumns.get(columnName);


            if (type.equals("P"))
            {
                table.primaryKeys.add(column);
                column.isPrimary = true;
            }

            if (type.equals("R"))
            {
                column.isForeign = true;
                column.refTable = rs.getString("RTNAME");
                column.refColumn = rs.getString("RCNAME");

                table.foreignKeys.add(column);
                String referenceTable = rs.getString("RTNAME");
                SQLTable.allTables.get(referenceTable).referencedBy.put(table.name, table);
                table.foreignKeysTables.put(referenceTable, SQLTable.allTables.get(referenceTable));
            }

            if(type.equals("U"))
            {
                table.uniques.add(column);
            }
        }

        stmt.close();

        for (SQLTableColumn column : table.allColumns.values())
        {
            if (!column.isPrimary && !column.isForeign)
                table.columns.add(column);
        }
    }

    public void begin(SQLTable table) throws SQLException
    {
        stmt = connection.createStatement();
        rs = stmt.executeQuery(String.format("SELECT * FROM %s", table.name));
    }

    public ResultSet next() throws SQLException
    {
        if(rs.next())
            return rs;
        else
            return null;
    }

    public void end() throws SQLException
    {
        stmt.close();
    }
}
