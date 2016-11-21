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

import java.net.URL;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.ResourceBundle;

public class Controller implements Initializable
{
    private Oracle oracle;

    private Mongo mongo;

    private ArrayList<Condition> conditions;

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
        manyToMany.setOnAction(actionEvent -> generateScript(Mongo.ScriptType.MANY));
        addCondition.setOnAction(actionEvent -> addCondition());
        clear.setOnAction(actionEvent ->
        {
            conditions.clear();
            conditionText.setText("");
            logic.getItems().setAll(Condition.LogicOperator.NOT);
        });
        indexes.setOnAction(actionEvent -> generateIndexes());
        run.setOnAction(actionEvent ->
        {
            if (tables.getValue() != null)
                script.setText(mongo.executeQuery(tables.getValue(), conditions));
        });
        logic.getItems().setAll(Condition.LogicOperator.NOT);
        conditions = new ArrayList<>();
    }

    private void generateScript(Mongo.ScriptType type)
    {
        if (tables.getValue() == null || type == null)
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

        refTables.getItems().setAll(table.foreignKeysTables.values());

        conditions.clear();
        logic.getItems().setAll(Condition.LogicOperator.NOT);
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
        condition.logicOperator = logic.getValue();
        condition.column = column.getValue();
        condition.operation = operation.getValue();
        condition.value = value.getText();
        if (conditions == null)
            conditions = new ArrayList<>();
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
                if(c.logicOperator == null)
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
        for(SQLTable table : SQLTable.allTables.values()){
            for(SQLTableColumn col : table.allColumns.values()){
                if(col.isPrimary || col.isForeign){
                    text += "db." + table.toString() + ".createIndex({" + col + ": 1})\n";
                }
            }
        }
        script.clear();
        script.setText(text);
    }
}