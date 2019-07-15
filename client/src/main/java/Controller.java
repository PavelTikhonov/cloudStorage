import javafx.application.Platform;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;


public class Controller implements Initializable {
    public VBox rootPane;
    @FXML
    private GridPane upperPanel;
    @FXML
    private TextField loginField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private HBox bottomPanel;
    @FXML
    private TableView<FileView> localStorage;
    @FXML
    private TableView<FileView> cloudStorage;
    @FXML
    private TableColumn<FileView, String> localFileName;
    @FXML
    private TableColumn<FileView, String> localFileSize;
    @FXML
    private TableColumn<FileView, String> cloudFileName;
    @FXML
    private TableColumn<FileView, String> cloudFileSize;
    @FXML
    private Label auth;

    private String clientWay = "abs/client_storage/";
    private String login;

    private ObservableList<FileView> localStorageData = FXCollections.observableArrayList(
            new FileView(" ", " ")
    );

    private ObservableList<FileView> cloudStorageData = FXCollections.observableArrayList(
            new FileView(" ", " ")
    );

    private void initStorage(){
        localFileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        localFileSize.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        localStorage.setItems(localStorageData);

        cloudFileName.setCellValueFactory(new PropertyValueFactory<>("fileName"));
        cloudFileSize.setCellValueFactory(new PropertyValueFactory<>("fileSize"));
        cloudStorage.setItems(cloudStorageData);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initStorage();
        refreshLocalFilesList();

        Network.start();

        Thread t = new Thread(() -> {
            try {
                while (true) {

                    AbstractMessage am = Network.readObject();
                    if(am instanceof AuthResult){
                        AuthResult ay = ((AuthResult) am);
                        if(ay.getResult().equals("ok")){
                            this.login = loginField.getText();
                            setAuthorized();
                        }
                    }
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get(clientWay + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
                        refreshLocalFilesList();
                    }
                    if (am instanceof FileList) {
                        FileList fl = (FileList) am;
                        cloudStorage.getItems().clear();
                        if (fl.getFileList() != null) {
                            for (FileDescription f : fl.getFileList()) {
                                cloudStorageData.add(new FileView(f.getFileName(), (f.getFileSize() + " B")));
                            }
                        }
                        if(isEmpty(cloudStorageData)){
                            cloudStorageData.add(new FileView("", ""));
                        }
                        cloudStorage.setItems(cloudStorageData);
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

    private void setAuthorized() {
        auth.setVisible(false);
        auth.setManaged(false);
        upperPanel.setVisible(false);
        upperPanel.setManaged(false);
        bottomPanel.setVisible(true);
        bottomPanel.setManaged(true);
    }

    private void refreshLocalFilesList() {
        localStorage.getItems().clear();
        File[] files = new File(clientWay).listFiles();
        if (files != null) {
            for (File f : files) {
                localStorageData.add(new FileView(f.getName(), (String.valueOf(f.length())) + " B"));
            }
        }
        if(isEmpty(localStorageData)){
            localStorageData.add(new FileView("", ""));
        }
        localStorage.setItems(localStorageData);
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

    public void pressLocalSendBtn(ActionEvent actionEvent) {
        FileView fw = localStorage.getSelectionModel().getSelectedItem();
        if(fw != null) {
            try {
                if(!fw.getFileName().equals("")) {
                    Network.sendMsg(new FileMessage(Paths.get(clientWay + fw.getFileName())));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public void pressLocalDeleteBtn(ActionEvent actionEvent) {
        FileView fw = localStorage.getSelectionModel().getSelectedItem();
        if(fw != null) {
            if(!fw.getFileName().equals("")) {
                File file = new File(clientWay + fw.getFileName());
                file.delete();
                refreshLocalFilesList();
            }
        }
    }

    public void pressCloudRefreshBtn(ActionEvent actionEvent){
        Network.sendMsg(new FileList());
    }

    public void pressCloudSendBtn(ActionEvent actionEvent) {
        FileView fw = cloudStorage.getSelectionModel().getSelectedItem();
        if(fw != null) {
            if(!fw.getFileName().equals("")) {
                Network.sendMsg(new FileRequest(fw.getFileName()));
            }
        }
    }

    public void pressCloudDeleteBtn(ActionEvent actionEvent) {
        FileView fw = cloudStorage.getSelectionModel().getSelectedItem();
        if (fw != null) {
            if(!fw.getFileName().equals("")) {
                Network.sendMsg(new FileDelete(fw.getFileName()));
            }
        }
    }

    private boolean isEmpty(ObservableList<FileView> ol){
        return ol.isEmpty();
    }


    public void tryToAuth(ActionEvent actionEvent) {
        if(Network.getSocket() != null || !Network.getSocket().isClosed()) {
            Network.sendMsg(new AuthRequest(loginField.getText(), passwordField.getText()));
        }
    }

    public String getLogin() {
        return login;
    }
}

