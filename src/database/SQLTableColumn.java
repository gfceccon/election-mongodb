package database;

import java.util.ArrayList;

public class SQLTableColumn
{
    public String name;
    public String type;

    public boolean isPrimary;
    public boolean isForeign;

    public ArrayList<SQLTableReference> references;

    public SQLTableColumn()
    {
        this.references = new ArrayList<>();
    }

    @Override
    public String toString()
    {
        return this.name;
    }
}