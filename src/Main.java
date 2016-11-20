import controller.Controller;
import controller.Login;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class Main extends Application
{

    @Override
    public void start(Stage stage) throws Exception
    {
        FXMLLoader loginLoader = new FXMLLoader();
        loginLoader.setLocation(Main.class.getResource("view/login.fxml"));
        Parent loginParent = loginLoader.load();

        FXMLLoader mainLoader = new FXMLLoader();
        mainLoader.setLocation(Main.class.getResource("view/mongo.fxml"));
        Parent mainParent = mainLoader.load();

        Login loginController = loginLoader.getController();
        Controller mainController = mainLoader.getController();
        loginController.setLoginFlow(stage, new Scene(mainParent), mainController);

        stage.setTitle("Login");
        stage.setScene(new Scene(loginParent));
        stage.show();
        stage.setResizable(false);
        stage.setOnCloseRequest(mainController.getCloseRequestEvent());
    }


    public static void main(String[] args)
    {
        launch(args);
    }
}