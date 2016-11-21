package database;

public class SQLTableReference
{
    public String refName;
    public SQLTable table;
    public String refColumn;

    public SQLTableReference(String refName, SQLTable table, String refColumn) {
        this.refName = refName;
        this.table = table;
        this.refColumn = refColumn;
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", refName, table.name);
    }
}
