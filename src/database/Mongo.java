package database;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

public class Mongo
{
    private MongoClient client;
    private MongoDatabase database;
    private static ArrayList<Operation> operations;

    public enum ScriptType
    {
        SIMPLE,
        REFERENCE,
        EMBEDDED,
        MANY
    }

    private Mongo()
    {
    }

    public static Mongo connect(String databaseName)
    {
        Mongo mongo;
        try
        {
            mongo = new Mongo();
            mongo.client = new MongoClient("localhost", 27017);
            mongo.database = mongo.client.getDatabase(databaseName);
            return mongo;
        } catch (Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }

    public static ArrayList<Operation> getOperations()
    {
        if (operations == null)
        {
            operations = new ArrayList<>();
            operations.add(new Operation(null, "="));
            operations.add(new Operation("$gt", ">"));
            operations.add(new Operation("$lt", "<"));
            operations.add(new Operation("$gte", ">="));
            operations.add(new Operation("$lte", "<="));
            operations.add(new Operation("$ne", "<>"));
        }
        return operations;
    }

    public void close()
    {
        client.close();
    }

/*
Caros, segue um exemplo apenas ilustrativo do código a ser gerado.
Notem que a entidade Candidatura irá precisar de uma solução específica, não incluída neste exemplo.
*/

/*Insert simples*/
    //db.createCollection("LE01Estado")
    //db.LE01Estado.insert({_id: "SP", nome:"Sao Paulo"})
    //db.LE01Estado.insert({_id: "RJ", nome:"Rio de Janeiro"})

/*Reference e Embedding*/
    //db.createCollection("LE02Cidade")
/*Exemplo reference*/
    //var doc = db.LE01Estado.findOne({_id:"SP"})
    //db.LE02Cidade.insert({_id:"SaoCarlos_SP",nome:"Sao Carlos",estado:doc._id})
/*Exemplo embeding -  nao haveria colecao para estado*/
    //db.LE02Cidade.insert({_id:"Volta_Redonda_RJ",nome:"Volta Redonda",estado:{sigla:"RJ", nome:"Rio de Janeiro"}})

/*N:N*/
    //db.createCollection("LE08Candidato")
    //db.LE08Candidato.insert({_id:11,tipo:"politico",CPF:12345,nome:"Joao"})
    //db.LE08Candidato.insert({_id:22,tipo:"politico",CPF:67890,nome:"Maria"})
    //db.createCollection("LE09Cargo")
    //db.LE09Cargo.insert({_id:1,NomeDescritivo:"Prefeito",ano:2010})
    //db.LE09Cargo.insert({_id:2,NomeDescritivo:"Prefeito",ano:2014})
    //db.LE08Candidato.update({_id:11},{$set:{candidaturas:[1,2]}})
    //db.LE08Candidato.update({_id:22},{$set:{candidaturas:[2]}})
    //db.LE09Cargo.update({_id:1},{$set:{candidatos:[11]}})
    //db.LE09Cargo.update({_id:2},{$set:{candidatos:[11,22]}})

    public String getScript(Oracle oracle, SQLTable table, ScriptType type) throws SQLException
    {
        MongoCollection collection = database.getCollection(table.name);
        StringBuilder builder = new StringBuilder();

        oracle.begin(table);

        ResultSet rs;
        while((rs = oracle.next()) != null)
        {
            switch (type)
            {
                case SIMPLE:
                    builder.append(String.format("db.%s.insert(%s)\n", table.name, getObjectSimple(rs, table).toJson()));
                    break;
                case REFERENCE:
                    builder.append(String.format("db.%s.insert(%s)\n", table.name, getObjectReference(rs, table).toJson()));
                    break;
                case EMBEDDED:
                    builder.append(String.format("db.%s.insert(%s)\n", table.name, getObjectEmbedded(rs, table).toJson()));
                    break;
                case MANY:
                    builder.append(String.format("db.%s.insert(%s)\n", table.name, getObjectMany(rs, table).toJson()));
                    break;
            }
        }

        oracle.end();

        return builder.toString();
    }

    private BasicDBObject getObjectMany(ResultSet rs, SQLTable table)
    {
        BasicDBObject dbObject = new BasicDBObject();
        BasicDBObject id = new BasicDBObject();



        return dbObject;
    }

    private BasicDBObject getObjectEmbedded(ResultSet rs, SQLTable table)
    {
        BasicDBObject dbObject = new BasicDBObject();
        BasicDBObject id = new BasicDBObject();



        return dbObject;
    }

    private BasicDBObject getObjectReference(ResultSet rs, SQLTable table)
    {
        BasicDBObject dbObject = new BasicDBObject();
        BasicDBObject id = new BasicDBObject();



        return dbObject;
    }

    public BasicDBObject getObjectSimple(ResultSet rs, SQLTable table) throws SQLException
    {
        BasicDBObject dbObject = new BasicDBObject();
        BasicDBObject id = new BasicDBObject();
        for (SQLTableColumn pk : table.primaryKeys)
            id.put(pk.name, rs.getObject(pk.name));
        dbObject.put("_id", id);

        for (SQLTableColumn col : table.columns)
        {
            Object value = rs.getObject(col.name);
            if(value != null)
                dbObject.put(col.name, value);
        }

        for (SQLTableColumn col : table.columns)
        {
            Object value = rs.getObject(col.name);
            if(value != null)
                dbObject.put(col.name, value);
        }
        return dbObject;
    }

    public BasicDBObject query(Collection<Condition> conditions)
    {
        if(conditions == null)
            return null;
        if(conditions.size() == 0)
            return null;

        BasicDBObject query = new BasicDBObject();
        if(conditions.size() == 1)
        {
            Condition condition = conditions.iterator().next();
            if(condition.logicOperator == Condition.LogicOperator.NOT)
            {
                BasicDBObject not = new BasicDBObject();
                putCondition(not, condition);
                query.put(Condition.LogicOperator.NOT.getLogicMongo(), not);
            }
            else
                putCondition(query, condition);
        }
        else
        {
            Iterator<Condition> iterator = conditions.iterator();
            Condition current = iterator.next();
            //Recursive case
            //query.put(next.logicOperator.getLogicMongo(), conditionRecursive(iterator,  current, next));

            //Simple case
            // NULL, COND, AND, COND, AND, COND, OR, COND, OR, COND, NOT, COND
            // OR (AND(COND, COND, COND), COND, COND, NOT(COND))
            BasicDBObject and = null;
            BasicDBObject not = null;
            BasicDBObject cond = new BasicDBObject();
            LinkedList<BasicDBObject> orList = new LinkedList<>();
            LinkedList<BasicDBObject> andList = new LinkedList<>();
            if(current.logicOperator == Condition.LogicOperator.NOT)
            {
                not = new BasicDBObject();
                putCondition(not, current);
                orList.add(new BasicDBObject(Condition.LogicOperator.NOT.getLogicMongo(), not));
            }
            else
            {
                putCondition(cond, current);
                orList.add(cond);
            }
            current = iterator.next();
            boolean quit = false;
            while(!quit)
            {
                if(current.logicOperator == Condition.LogicOperator.AND || current.logicOperator == Condition.LogicOperator.AND_NOT)
                {
                    if(and == null)
                    {
                        andList.clear();
                        andList.add(orList.pollLast());
                        and = new BasicDBObject();
                    }
                    if(current.logicOperator == Condition.LogicOperator.AND_NOT)
                    {
                        not = new BasicDBObject();
                        putCondition(not, current);
                        andList.add(new BasicDBObject(Condition.LogicOperator.NOT.getLogicMongo(), not));
                    }
                    else
                    {
                        cond = new BasicDBObject();
                        putCondition(cond, current);
                        andList.add(cond);
                    }
                }
                else
                {
                    if(and != null)
                    {
                        and.put(Condition.LogicOperator.AND.getLogicMongo(), andList);
                        orList.add(and);
                    }
                    and = null;
                    if(current.logicOperator == Condition.LogicOperator.OR_NOT)
                    {
                        not = new BasicDBObject();
                        putCondition(not, current);
                        orList.add(new BasicDBObject(Condition.LogicOperator.NOT.getLogicMongo(), not));
                    }
                    else
                    {
                        cond = new BasicDBObject();
                        putCondition(cond, current);
                        orList.add(cond);
                    }
                }
                if(!iterator.hasNext())
                    quit = true;
                else
                    current = iterator.next();
            }
            if(and != null)
            {
                and.put(Condition.LogicOperator.AND.getLogicMongo(), andList);
                orList.add(and);
            }
            if(orList.size() == 1)
                query = and;
            else
                query.put(Condition.LogicOperator.OR.getLogicMongo(), orList);
        }

        return query;
    }

    private void putCondition(BasicDBObject obj, Condition condition)
    {
        Object value = condition.value;
        //DateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);


        switch (condition.column.type)
        {
            case "DATE":
                try {
                    Date df = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse(condition.value);
                    value = df;

                } catch(Exception e) {
                    e.printStackTrace();
                }
                break;
            case "NUMBER":
                value = Double.parseDouble(condition.value);
                break;
        }
        if(condition.operation.mongo == null)
            obj.put(condition.column.name.toLowerCase(), value);
        else
            obj.put(condition.column.name.toLowerCase(), new BasicDBObject(condition.operation.mongo, value));
    }


    // NULL, COND, AND, COND, OR, COND
    // AND (COND, AND (COND, AND (COND, COND)))
    private Object conditionRecursive(Iterator<Condition> iterator, Condition current, Condition next)
    {
        BasicDBObject result = new BasicDBObject();
        if(!iterator.hasNext())
        {
            putCondition(result, current);
            putCondition(result, next);
            return result;
        }
        Object obj = conditionRecursive(iterator, next, iterator.next());
        putCondition(result, current);
        result.put(next.logicOperator.getLogicMongo(), obj);
        return obj;
    }

    public String executeQuery(SQLTable table, Collection<Condition> conditions)
    {
        String builder = "";
        MongoCollection collection = database.getCollection(table.name);

        MongoCursor<Document> cursor = collection.find(this.query(conditions)).iterator();

        while(cursor.hasNext()){
            builder += cursor.next().toJson() + "\n";
        }

        return builder;
    }
}
