import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainStage extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{

        FXMLLoader loader = new FXMLLoader(MainStage.class.getResource("/sample.fxml"));
        Parent root = loader.load();
        MainController controller = loader.getController();
        primaryStage.setTitle("Cloud Storage");
        primaryStage.getIcons().add(new Image("/cloudsolutions.jpg"));
        primaryStage.setScene(new Scene(root, 600, 470));
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            if (Network.getSocket() != null) {
                Network.sendMsg(new AuthClose(controller.getLogin()));
            }
        });

    }


    public static void main(String[] args) {
        launch(args);
    }
}
