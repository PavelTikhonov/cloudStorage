import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.FileDescriptor;
import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

public class Controller implements Initializable {
    public HBox upperPanel;
    public TextField loginField;
    public PasswordField passwordField;
    public HBox bottomPanel;
    public TableView<FileView> localStorage;
    public TableView<FileView> cloudStorage;
    public TableColumn<FileView, String> localFileName;
    public TableColumn<FileView, String> localFileSize;
    public TableColumn<FileView, String> cloudFileName;
    public TableColumn<FileView, String> cloudFileSize;
    public VBox localVBox;
    public Button localBtnSendFile;
    public Button localBtnDeleteFile;
    public Button localBtnRefreshFile;
    public Button cloudBtnRefreshFile;
    public Button cloudBtnDeleteFile;
    public Button cloudBtnSendFile;

    private ObservableList<FileView> dataD = FXCollections.observableArrayList(
            new FileView(" ", " ")
    );

    private ObservableList<FileView> data1 = FXCollections.observableArrayList(
            new FileView(" ", " ")
    );

    public void initStorages(){
        localFileName.setCellValueFactory(new PropertyValueFactory<FileView, String>("fileName"));
        localFileSize.setCellValueFactory(new PropertyValueFactory<FileView, String>("fileSize"));
        localStorage.setItems(dataD);

        cloudFileName.setCellValueFactory(new PropertyValueFactory<FileView, String>("fileName"));
        cloudFileSize.setCellValueFactory(new PropertyValueFactory<FileView, String>("fileSize"));
        cloudStorage.setItems(data1);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initStorages();

        Network.start();
        Thread t = new Thread(() -> {
            try {
                while (true) {

                    AbstractMessage am = Network.readObject();
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
//                        Files.write(Paths.get("client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        refreshLocalFilesList();
                    }
                }
            } catch (ClassNotFoundException | IOException e) {
                e.printStackTrace();
            } finally {
                Network.stop();
            }
        });
        t.setDaemon(true);
        t.start();

    }

//    public void pressOnDownloadBtn(ActionEvent actionEvent) {
//        if (tfFileName.getLength() > 0) {
//            Network.sendMsg(new FileRequest(tfFileName.getText()));
//            tfFileName.clear();
//        }
//    }
//
    public void refreshLocalFilesList() {
        updateUI(() -> {
            localStorage.getItems().clear();
            File[] files = new File("abs/client_storage").listFiles();
            for (File f : files) {
                dataD.add(new FileView(f.getName(), (String.valueOf(f.length())) + " B"));
            }
            localStorage.setItems(dataD);
        });
    }

    public static void updateUI(Runnable r) {
        if (Platform.isFxApplicationThread()) {
            r.run();
        } else {
            Platform.runLater(r);
        }
    }


    public void pressLocalRefreshBtn(ActionEvent actionEvent) {
        refreshLocalFilesList();
    }
}
