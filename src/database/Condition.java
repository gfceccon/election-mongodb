package database;

import java.util.ArrayList;

public class Condition
{
    public enum LogicOperator
    {
        AND,
        OR,
        NOT,
        AND_NOT,
        OR_NOT,
        NULLEMPTY;

        private static ArrayList<LogicOperator> all;

        public static ArrayList<LogicOperator> getAll()
        {
            if(all == null)
            {
                all = new ArrayList<>();
                all.add(AND);
                all.add(OR);
                all.add(AND_NOT);
                all.add(OR_NOT);
            }
            return all;
        }

        @Override
        public String toString()
        {
            switch (this)
            {
                case AND:
                    return "AND";
                case OR:
                    return "OR";
                case NOT:
                    return "NOT";
                case AND_NOT:
                    return "AND NOT";
                case OR_NOT:
                    return "OR NOT";
                case NULLEMPTY:
                    return " ";
            }
            return "";
        }

        public String getLogicMongo()
        {
            switch (this)
            {
                case AND:
                    return "$and";
                case OR:
                    return "$or";
                case NOT:
                    return "$not";
                case AND_NOT:
                    return "$not";
                case OR_NOT:
                    return "$not";
            }
            return "";
        }
    }

    public LogicOperator logicOperator;
    public SQLTableColumn column;
    public Operation operation;
    public String value;
}
