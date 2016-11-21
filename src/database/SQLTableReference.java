package database;

public class SQLTableReference
{
    public String refName;
    public SQLTable table;

    public SQLTableReference(String refName, SQLTable table) {
        this.refName = refName;
        this.table = table;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", refName, table.name);
    }
}
