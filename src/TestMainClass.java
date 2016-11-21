import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import java.io.RandomAccessFile;
import java.security.SecureRandom;
import java.util.*;

public class TestMainClass {

    public static MongoClient mongo;
    public static MongoDatabase db;
    public static MongoCollection table;
    public static ArrayList<Document> tuples = new ArrayList<Document>();

    static final String AB = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz ";
    static SecureRandom rnd = new SecureRandom();

    public static String randomString( int len ){
        StringBuilder sb = new StringBuilder( len );
        for( int i = 0; i < len; i++ )
            sb.append( AB.charAt( rnd.nextInt(AB.length()) ) );
        return sb.toString();
    }

    public static void insertIntoCollection(int num) throws Exception {
        Document doc = new Document();
        String rand = null;

        if(tuples.size() < num) {
            for (int i = 0; i < num; i++) {
                doc.put("_id", i);
                rand = randomString(2000);
                doc.put("nome1", rand);
                rand = randomString(2000);
                doc.put("nome2", rand);
                rand = randomString(2000);
                doc.put("nome3", rand);
                rand = randomString(2000);
                doc.put("nome4", rand);
                rand = randomString(2000);
                doc.put("nome5", rand);
                rand = randomString(2000);
                doc.put("nome6", rand);
                rand = randomString(2000);
                doc.put("nome7", rand);
                rand = randomString(2000);
                doc.put("nome8", rand);
                rand = randomString(2000);
                doc.put("nome9", rand);
                rand = randomString(2000);
                doc.put("nome10", rand);
                rand = randomString(2000);
                doc.put("nome11", rand);

                tuples.add(doc);
                doc = new Document();
            }
        }

        long startTime = System.nanoTime();
        table.insertMany(tuples);
        System.out.println("Time elapsed for " + num + " inserts: " + ((System.nanoTime() - startTime)/ 1000000000.0) + "s");
    }

    public static void findValues(int num){
        Random rn = new Random();
        Document doc = new Document();


        long startTime = System.nanoTime();
        for(int i = 0; i < num; i++){
            doc.put("nome1",tuples.get(rn.nextInt(num)).get("nome1"));

            table.find(doc);
        }
        System.out.println("Time elapsed for " + num + " selects: " + ((System.nanoTime() - startTime)/ 1000000000.0) + "s");
    }

    public static void createIndexes(){
        BasicDBObject index1 = new BasicDBObject("name1",1);

        table.createIndex(index1);
    }

	public static void main(String[] args) {
        // TODO code application logic here
        try {             /*Connect*/
            mongo = new MongoClient("localhost", 27017);
            db = mongo.getDatabase("labbd2016");
            int num = 10000;

            //TABELA SEM INDICE
            table = db.getCollection("teste_100");
            table.drop();
            System.out.println("NO INDEX");
            insertIntoCollection(num);
            findValues(num);

            //TABELA COM INDICE
            table = db.getCollection("teste_100_index");
            table.drop();
            createIndexes();
            System.out.println("WITH INDEX");
            insertIntoCollection(num);
            findValues(num);
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

}
