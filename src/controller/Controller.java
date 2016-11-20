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
    public ChoiceBox<SQLTable> table;
    @FXML
    public Button simple;
    @FXML
    public Button reference;
    @FXML
    public Button embedded;
    @FXML
    public Button manyToMany;
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
            table.getItems().addAll(oracle.getTables());
        } catch (SQLException e)
        {
            script.setText(e.getMessage());
        }
    }

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        table.valueProperty().addListener((observable, oldValue, newValue) -> setTable(newValue));
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
        run.setOnAction(actionEvent ->
        {
            if (table.getValue() != null)
                script.setText(mongo.executeQuery(table.getValue(), mongo.query(conditions)));
        });
        logic.getItems().setAll(Condition.LogicOperator.NOT);
        conditions = new ArrayList<>();
    }

    private void generateScript(Mongo.ScriptType type)
    {
        if (table.getValue() == null || type == null)
            return;
        String text = null;
        try
        {
            text = mongo.getScript(oracle, table.getValue(), type);
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

        script.setText(mongo.query(conditions).toJson());
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
}