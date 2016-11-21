package database;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;
import org.bson.Document;

import javax.print.Doc;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

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

    public String getScript(Oracle oracle, SQLTable table, SQLTableReference refTable, ScriptType type) throws SQLException
    {
        //MongoCollection collection = database.getCollection(table.name);
        StringBuilder builder = new StringBuilder();

        oracle.begin(table);

        ResultSet rs;


        List<SQLTableColumn> fks = table.allColumns.values().stream().filter(
                col -> col.references.stream().anyMatch(
                        tableReference -> tableReference.refName.equals(refTable.refName))).collect(Collectors.toList());
        while((rs = oracle.next()) != null)
        {
            switch (type)
            {
                case SIMPLE:
                    builder.append(String.format("db.%s.insert(%s)\n", table.name, getObjectSimple(rs, table).toJson()));
                    break;
                case REFERENCE:
                    builder.append(String.format("db.%s.insert(%s)\n", table.name, getObjectReference(rs, table, refTable.refName, fks).toJson()));
                    break;
                case EMBEDDED:
                    builder.append(String.format("db.%s.insert(%s)\n", table.name, getObjectEmbedded(rs, table, refTable.refName, fks).toJson()));
                    break;
            }
        }

        oracle.end();

        return builder.toString();
    }

    public String generateScript(Oracle oracle, SQLTable table, SQLTableReference[] refTables) throws Exception
    {
        if(refTables.length < 2)
            throw new Exception("Reference tables size less than 2 on N:N");


        //MongoCollection collection = database.getCollection(table.name);
        StringBuilder builder = new StringBuilder();

        oracle.begin(table);

        ResultSet rs;


        List<SQLTableColumn> right = table.allColumns.values().stream().filter(
                col -> col.references.stream().anyMatch(
                        tableReference -> tableReference.refName.equals(refTables[0].refName))).collect(Collectors.toList());

        List<SQLTableColumn> left = table.allColumns.values().stream().filter(
                col -> col.references.stream().anyMatch(
                        tableReference -> tableReference.refName.equals(refTables[1].refName))).collect(Collectors.toList());

        while((rs = oracle.next()) != null)
            builder.append(String.format("db.%s.insert(%s)\n", table.name, getObjectMany(rs, table, right, left)));

        oracle.end();

        return builder.toString();
    }

    private Object getBSONType(ResultSet rs, SQLTableColumn column) throws SQLException
    {
        switch (column.type)
        {
            case "NUMBER":
                return rs.getDouble(column.name);
            case "DATE":
                return rs.getDate(column.name);
            case "CHAR":
            case "VARCHAR":
                return rs.getString(column.name);
        }
        return rs.getObject(column.name);
    }

    private BasicDBObject getObjectMany(ResultSet rs, SQLTable table, List<SQLTableColumn> rightFks, List<SQLTableColumn> leftFks) throws SQLException {
        BasicDBObject dbObject = new BasicDBObject();
        BasicDBObject id = new BasicDBObject();

        for (SQLTableColumn pk : table.primaryKeys)
            id.put(pk.name, rs.getObject(pk.name));
        dbObject.put("_id", id);
        dbObject.put("_id", id);


        return dbObject;
    }

    private BasicDBObject getObjectEmbedded(ResultSet rs, SQLTable table, String refName, List<SQLTableColumn> fks) throws SQLException {
        BasicDBObject dbObject = new BasicDBObject();
        BasicDBObject id = new BasicDBObject();

        for (SQLTableColumn pk : table.primaryKeys)
            id.put(pk.name, getBSONType(rs, pk));
        dbObject.put("_id", id);

        return dbObject;
    }

    private BasicDBObject getObjectReference(ResultSet rs, SQLTable table, String refName, List<SQLTableColumn> fks) throws SQLException {
        BasicDBObject dbObject = new BasicDBObject();
        BasicDBObject id = new BasicDBObject();
        BasicDBObject ref = new BasicDBObject();

        boolean isPK = false;
        int fkCount = 0;

        for (SQLTableColumn pk : table.primaryKeys)
        {
            if(pk.isForeign && fks.contains(pk))
            {
                ref.put(pk.name, getBSONType(rs, pk));
                fkCount++;
                isPK = true;
            }
            else
                id.put(pk.name, getBSONType(rs, pk));
        }
        if(isPK && fks.size() == fkCount)
            id.put(refName, ref);
        else if(isPK)
            id.putAll(ref.toMap());
        dbObject.put("_id", id);

        fkCount = 0;
        for (SQLTableColumn fk : table.foreignKeys)
        {
            Object value = getBSONType(rs, fk);
            if(fk.isPrimary)
                continue;
            if (value != null)
            {
                if(fks.contains(fk))
                {
                    ref.put(fk.name, value);
                    fkCount++;
                }
                else
                    dbObject.put(fk.name, value);
            }
        }
        if(fks.size() == fkCount)
            dbObject.put(refName, ref);
        else if(ref.size() > 0)
            dbObject.putAll(ref.toMap());

        for (SQLTableColumn col : table.columns)
        {
            Object value = getBSONType(rs, col);
            if(value != null)
                dbObject.put(col.name, value);
        }

        return dbObject;
    }

    public BasicDBObject getObjectSimple(ResultSet rs, SQLTable table) throws SQLException
    {
        BasicDBObject dbObject = new BasicDBObject();
        BasicDBObject id = new BasicDBObject();

        for (SQLTableColumn pk : table.primaryKeys)
            id.put(pk.name, getBSONType(rs, pk));
        dbObject.put("_id", id);

        for (SQLTableColumn fk : table.foreignKeys)
        {
            Object value = getBSONType(rs, fk);
            if(value != null)
                dbObject.put(fk.name, value);
        }

        for (SQLTableColumn col : table.columns)
        {
            Object value = getBSONType(rs, col);
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

            // NULLEMPTY, COND, AND, COND, AND, COND, OR, COND, OR, COND, NOT, COND
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
                    value = new SimpleDateFormat("dd/MM/yyyy", Locale.ENGLISH).parse(condition.value);

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

    public String executeQuery(SQLTable table, Collection<Condition> conditions)
    {
        String builder = "";
        MongoCollection<Document> collection = database.getCollection(table.name);

        for (Document document : collection.find(this.query(conditions)))
            builder += document.toJson() + "\n";

        return builder;
    }
}
