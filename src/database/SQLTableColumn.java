package database;

import java.util.ArrayList;

public class SQLTableColumn
{
    public String name;
    public String type;

    public boolean isPrimary;
    public boolean isForeign;

    public String refTable;
    public String refColumn;

    @Override
    public String toString()
    {
        return this.name;
    }
}