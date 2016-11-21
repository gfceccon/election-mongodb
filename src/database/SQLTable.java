package database;

import java.util.HashMap;
import java.util.ArrayList;

public class SQLTable
{
    public enum TableType
    {
        USER,
        VIEW,
        MVIEW;

        public String getQuery()
        {
            switch (this)
            {
                case USER:
                    return "SELECT TABLE_NAME AS NAME FROM USER_TABLES MINUS SELECT MVIEW_NAME AS NAME FROM USER_MVIEWS";
                case VIEW:
                    return "SELECT VIEW_NAME AS NAME FROM USER_VIEWS";
                case MVIEW:
                    return "SELECT MVIEW_NAME AS NAME FROM USER_MVIEWS";
            }
            return "";
        }
    }

    public static HashMap<String, SQLTable> allTables = new HashMap<>();

    public String name;
    public TableType type;
    public HashMap<String, SQLTableColumn> allColumns;
    public ArrayList<SQLTableColumn> columns;
    public ArrayList<SQLTableColumn> primaryKeys;
    public ArrayList<SQLTableColumn> foreignKeys;
    public ArrayList<SQLTableColumn> uniques;
    public HashMap<String, SQLTable> referencedBy;
    public HashMap<String, SQLTableReference> foreignKeysTables;

    public SQLTable(String name, TableType type)
    {
        this.name = name;
        this.type = type;
        this.allColumns = new HashMap<>();
        this.columns = new ArrayList<>();
        this.primaryKeys = new ArrayList<>();
        this.foreignKeys = new ArrayList<>();
        this.uniques = new ArrayList<>();
        this.referencedBy = new HashMap<>();
        this.foreignKeysTables = new HashMap<>();
    }

    @Override
    public String toString()
    {
        String name = "";
        switch (type)
        {
            case VIEW:
                name = "(VIEW)";
                break;
            case MVIEW:
                name = "(MATERIALIZED)";
        }
        name += this.name;
        return name;
    }
}