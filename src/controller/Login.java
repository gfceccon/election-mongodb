package controller;

import database.Mongo;
import database.Oracle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.net.URL;
import java.sql.SQLException;
import java.util.ResourceBundle;

public class Login implements Initializable
{
    @FXML
    public Button loginButton;

    @FXML
    public TextField user;

    @FXML
    public TextField database;

    @FXML
    public PasswordField password;

    private Stage stage;
    private Scene scene;
    private Controller controller;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle)
    {
        loginButton.setDefaultButton(true);
        loginButton.onActionProperty().set(actionEvent ->
        {
            try
            {
                Oracle oracle = Oracle.connect(user.getText(), password.getText());
                Mongo mongo = Mongo.connect(database.getText());

                stage.setTitle("Election - MongoDB");

                stage.setWidth(800);
                stage.setHeight(600);

                stage.setResizable(true);
                stage.setScene(scene);
                stage.show();

                controller.setOracle(oracle);
                controller.setMongo(mongo);
            } catch (ClassNotFoundException | SQLException e)
            {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Login Error");
                alert.setHeaderText("Error on authentication or driver not found!");
                alert.showAndWait();
            }
        });
    }

    public void setLoginFlow(Stage stage, Scene scene, Controller controller)
    {
        this.stage = stage;
        this.scene = scene;
        this.controller = controller;
    }
}