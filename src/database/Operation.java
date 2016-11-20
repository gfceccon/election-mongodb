package database;

public class Operation
{
    public String mongo;
    public String name;

    public Operation(String mongo, String name)
    {
        this.mongo = mongo;
        this.name = name;
    }

    @Override
    public String toString()
    {
        return name;
    }
}
