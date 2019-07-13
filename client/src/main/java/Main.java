import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        Parent root = FXMLLoader.load(getClass().getResource("/sample.fxml"));
        primaryStage.setTitle("Cloud Storage");
        primaryStage.getIcons().add(new Image("/cloudsolutions.jpg"));
        primaryStage.setScene(new Scene(root, 600, 470));
        primaryStage.show();
    }


    public static void main(String[] args) {
        launch(args);
    }
}
