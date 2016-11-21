package controller;

import database.*;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.WindowEvent;
import org.bson.json.JsonMode;
import org.bson.json.JsonWriterSettings;

import java.io.*;
import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.*;
import java.util.stream.Collectors;

public class Controller implements Initializable
{
    private Oracle oracle;

    private Mongo mongo;

    private ArrayList<Condition> conditions;

    private SQLTableReference[] manyToManyReferences;

    private int manyToManySize;

    @FXML
    public ChoiceBox<SQLTable> tables;
    @FXML
    public ChoiceBox<SQLTableReference> refTables;
    @FXML
    public Button simple;
    @FXML
    public Button reference;
    @FXML
    public Button embedded;
    @FXML
    public Button manyToMany;
    @FXML
    public Button indexes;
    @FXML
    public Button validation;
    @FXML
    public TextArea script;
    @FXML
    public ChoiceBox<Condition.LogicOperator> logic;
    @FXML
    public ChoiceBox<SQLTableColumn> column;
    @FXML
    public ChoiceBox<Operation> operation;
    @FXML
    public TextField value;
    @FXML
    public Button addCondition;
    @FXML
    public Button clear;
    @FXML
    public Button run;
    @FXML
    public TextField conditionText;

    public void setMongo(Mongo mongo)
    {
        this.mongo = mongo;
    }

    public void setOracle(Oracle oracle)
    {
        this.oracle = oracle;
        try
        {
            tables.getItems().addAll(oracle.getTables());
        } catch (SQLException e)
        {
            script.setText(e.getMessage());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        tables.valueProperty().addListener((observable, oldValue, newValue) -> setTable(newValue));
        operation.getItems().addAll(Mongo.getOperations());
        simple.setOnAction(actionEvent -> generateScript(Mongo.ScriptType.SIMPLE));
        reference.setOnAction(actionEvent -> generateScript(Mongo.ScriptType.REFERENCE));
        embedded.setOnAction(actionEvent -> generateScript(Mongo.ScriptType.EMBEDDED));
        manyToMany.setOnAction(actionEvent -> addManyToMany());
        addCondition.setOnAction(actionEvent -> addCondition());
        validation.setOnAction(actionEvent -> showValidation());
        clear.setOnAction(actionEvent -> clear());
        indexes.setOnAction(actionEvent -> generateIndexes());
        run.setOnAction(actionEvent ->
        {
            if (tables.getValue() != null)
                script.setText(mongo.executeQuery(tables.getValue(), conditions));
        });
        conditions = new ArrayList<>();
        manyToManyReferences = new SQLTableReference[2];
        clear();
    }
    private void clear()
    {
        script.clear();
        manyToManySize = 0;

        conditions.clear();
        conditionText.clear();

        logic.getItems().setAll(Condition.LogicOperator.NULLEMPTY, Condition.LogicOperator.NOT);
    }

    private void addManyToMany()
    {
        if (tables.getValue() == null || refTables.getValue() == null)
            return;
        manyToManyReferences[manyToManySize++] = refTables.getValue();
        if(manyToManySize == 2)
        {
            String text = null;
            simple.setDisable(false);
            reference.setDisable(false);
            embedded.setDisable(false);
            indexes.setDisable(false);
            validation.setDisable(false);
            addCondition.setDisable(false);
            run.setDisable(false);
            try
            {
                text = mongo.generateScript(oracle, tables.getValue(), manyToManyReferences);
            } catch (Exception e)
            {
                script.setText(e.getMessage());
            }
            script.setText(text);
            setTable(tables.getValue());
        }
        else
        {
            simple.setDisable(true);
            reference.setDisable(true);
            embedded.setDisable(true);
            indexes.setDisable(true);
            validation.setDisable(true);
            addCondition.setDisable(true);
            run.setDisable(true);
            refTables.getItems().remove(refTables.getValue());
            refTables.setValue(null);
            script.setText(String.format("%s -> %s (%s)", tables.getValue().name, manyToManyReferences[0].table.name, manyToManyReferences[0].refName));
        }
    }

    private void generateScript(Mongo.ScriptType type)
    {
        clear();
        if (tables.getValue() == null || type == null)
            return;
        if(type != Mongo.ScriptType.SIMPLE && refTables.getValue() == null)
            return;
        String text = null;
        try
        {
            text = mongo.getScript(oracle, tables.getValue(), refTables.getValue(), type);
        } catch (SQLException e)
        {
            script.setText(e.getMessage());
        }
        script.setText(text);
    }

    private void setTable(SQLTable table)
    {
        column.getItems().clear();
        column.getItems().addAll(table.allColumns.values());

        refTables.getItems().setAll(table.foreignKeysTables.stream().filter(distinctByKey(tableReference -> tableReference.refName)).collect(Collectors.toList()));

        clear();
    }

    private void addCondition()
    {
        if(conditions.size() > 0 && logic.getValue() == null)
            return;
        if (column.getValue() == null || operation.getValue() == null)
            return;
        if(value.getText().isEmpty())
            return;
        Condition condition = new Condition();
        if(condition.logicOperator == Condition.LogicOperator.NULLEMPTY)
            condition.logicOperator = null;
        else
            condition.logicOperator = logic.getValue();
        condition.column = column.getValue();
        condition.operation = operation.getValue();
        condition.value = value.getText();
        conditions.add(condition);

        if(conditions.size() == 1)
            logic.getItems().setAll(Condition.LogicOperator.getAll());

        script.setText(mongo.query(conditions).toJson(new JsonWriterSettings(JsonMode.SHELL)));
        boolean first = true;
        String logic = "";
        for (Condition c: conditions)
        {
            if(first)
            {
                if(c.logicOperator == Condition.LogicOperator.NULLEMPTY || c.logicOperator == null)
                    logic = "" + c.column + c.operation + c.value;
                else
                    logic = "NOT " + c.column + c.operation + c.value;
            }
            else
                switch (c.logicOperator)
                {
                    case AND:
                            logic += " AND " + c.column + c.operation + c.value;
                        break;
                    case OR:
                            logic += " AND " + c.column + c.operation + c.value;
                        break;
                    case AND_NOT:
                            logic += " AND NOT " + c.column + c.operation + c.value;
                        break;
                    case OR_NOT:
                            logic += " OR NOT " + c.column + c.operation + c.value;
                        break;
                }
            first = false;
        }
        conditionText.setText(logic);
    }

    public EventHandler<WindowEvent> getCloseRequestEvent()
    {
        return windowEvent ->
        {
            try
            {
                if (oracle != null)
                    oracle.close();
                if (mongo != null)
                    mongo.close();
            } catch (SQLException e)
            {
                e.printStackTrace();
            }
        };
    }

    public void generateIndexes(){
        String text = "";
        int count;

        clear();

        for(SQLTable table : SQLTable.allTables.values()){
            count = 0;
            text += "db." + table.toString() + ".createIndex({";
            for(SQLTableColumn col : table.uniques){
                if(count>0)
                    text += "," + col + ": 1";
                else
                    text += col + ": 1";
                count++;
            }
            if(count > 0)
                text += "}, {unique: true, sparse: true})\n";
            else
                text = "";

            script.appendText(text);
            text = "";
        }
    }

    public void showValidation(){
        try {
            String text = "";
            FileInputStream fl = new FileInputStream("./res/ex3.txt");
            BufferedReader br = new BufferedReader(new InputStreamReader(fl));

            clear();
            while((text = br.readLine())!= null){
                script.appendText(text + "\n");
            }

            fl.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object,Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }
}