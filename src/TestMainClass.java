import org.bson.Document;

import com.mongodb.BasicDBObject;
import com.mongodb.MongoClient;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.MongoDatabase;

import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Random;

public class TestMainClass {

    public static MongoClient mongo;
    public static MongoDatabase db;
    public static MongoCollection table;
    public static ArrayList<Document> tuples = new ArrayList<Document>();

    public static void InsertIntoCollection(int num) throws Exception {
        RandomAccessFile fr = new RandomAccessFile("C:\\Users\\mathe\\Documents\\GitHub\\election-mongodb\\res\\lorem.txt", "r");
        Document doc = new Document();
        String rand = new String();
        Random rn = new Random();

        if(tuples.size() < num) {
            for (int i = 0; i < num; i++) {
                doc.put("_id", i);
                fr.seek(rn.nextInt((int) fr.length()));
                rand = fr.readLine();
                doc.put("nome1", rand);
                fr.seek(rn.nextInt((int) fr.length()));
                rand = fr.readLine();
                doc.put("nome2", rand);
                fr.seek(rn.nextInt((int) fr.length()));
                rand = fr.readLine();
                doc.put("nome3", rand);
                fr.seek(rn.nextInt((int) fr.length()));
                rand = fr.readLine();
                doc.put("nome4", rand);
                fr.seek(rn.nextInt((int) fr.length()));
                rand = fr.readLine();
                doc.put("nome5", rand);
                fr.seek(rn.nextInt((int) fr.length()));
                rand = fr.readLine();
                doc.put("nome6", rand);
                fr.seek(rn.nextInt((int) fr.length()));
                rand = fr.readLine();
                doc.put("nome7", rand);
                fr.seek(rn.nextInt((int) fr.length()));
                rand = fr.readLine();
                doc.put("nome8", rand);
                fr.seek(rn.nextInt((int) fr.length()));
                rand = fr.readLine();
                doc.put("nome9", rand);
                fr.seek(rn.nextInt((int) fr.length()));
                rand = fr.readLine();
                doc.put("nome10", rand);
                fr.seek(rn.nextInt((int) fr.length()));
                rand = fr.readLine();
                doc.put("nome11", rand);

                tuples.add(doc);
                doc = new Document();
            }
        }

        long startTime = System.nanoTime();
        table.insertMany(tuples);
        System.out.println("Time elapsed for " + num + " inserts: " + ((System.nanoTime() - startTime)/ 1000000000.0) + "s");
    }

    public static void FindValues(int num){
        Random rn = new Random();
        
        long startTime = System.nanoTime();
        for(int i = 0; i < num; i++){
            table.find(tuples.get(rn.nextInt(num)));
        }
        System.out.println("Time elapsed for " + num + " selects: " + ((System.nanoTime() - startTime)/ 1000000000.0) + "s");
    }

    public static void CreateIndexes(){
        BasicDBObject index1 = new BasicDBObject("name1",1);

        table.createIndex(index1);
    }

	public static void main(String[] args) {
        // TODO code application logic here
        try {             /*Connect*/
            mongo = new MongoClient("localhost", 27017);
            db = mongo.getDatabase("labbd2016");
            int num = 1000;

            //TABELA SEM INDICE
            table = db.getCollection("teste_100");
            table.drop();
            InsertIntoCollection(num);
            FindValues(num);

            //TABELA COM INDICE
            /*table = db.getCollection("teste_100_index");
            table.drop();
            CreateIndexes();
            InsertIntoCollection(num);
            FindValues(num);*/
        } catch (Exception e) {
            e.printStackTrace();
        }
	}

}
