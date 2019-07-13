import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.collections.*;
import javafx.event.ActionEvent;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.stream.Collectors;

public class Controller implements Initializable {
    public VBox upperPanel;
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

    private boolean isAuthorized;

    private ObservableList<FileView> localStorageData = FXCollections.observableArrayList(
            new FileView(" ", " ")
    );

    private ObservableList<FileView> cloudStorageData = FXCollections.observableArrayList(
            new FileView(" ", " ")
    );

    private void initStorage(){
        localFileName.setCellValueFactory(new PropertyValueFactory<FileView, String>("fileName"));
        localFileSize.setCellValueFactory(new PropertyValueFactory<FileView, String>("fileSize"));
        localStorage.setItems(localStorageData);

        cloudFileName.setCellValueFactory(new PropertyValueFactory<FileView, String>("fileName"));
        cloudFileSize.setCellValueFactory(new PropertyValueFactory<FileView, String>("fileSize"));
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
                            setAuthorized();
                        }
                    }
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        Files.write(Paths.get("abs/client_storage/" + fm.getFilename()), fm.getData(), StandardOpenOption.CREATE);
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
        upperPanel.setVisible(false);
        upperPanel.setManaged(false);
        bottomPanel.setVisible(true);
        bottomPanel.setManaged(true);
    }

    private void refreshLocalFilesList() {
        localStorage.getItems().clear();
        File[] files = new File("abs/client_storage").listFiles();
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
                    Network.sendMsg(new FileMessage(Paths.get("abs/client_storage/" + fw.getFileName())));
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
                File file = new File("abs/client_storage/" + fw.getFileName());
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
}
