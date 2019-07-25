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

import java.io.*;
import java.net.URL;
import java.util.Arrays;
import java.util.ResourceBundle;


public class MainController implements Initializable {
    public VBox rootPane;
    @FXML
    private ProgressBar loadBarProgress;
    @FXML
    private GridPane loadPanel;
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
    private String tableEmpty = "Нет файлов в хранилище";
    private String login;
    private boolean isExchangedFiles;

    private ObservableList<FileView> localStorageData = FXCollections.observableArrayList(
            new FileView(" ", " ")
    );

    private ObservableList<FileView> cloudStorageData = FXCollections.observableArrayList(
            new FileView(tableEmpty, tableEmpty)
    );

    private void initStorage(){
        localFileName.setCellValueFactory(new PropertyValueFactory<FileView, String >("fileName"));
        localFileSize.setCellValueFactory(new PropertyValueFactory<FileView, String>("fileSize"));
        localStorage.setItems(localStorageData);

        cloudFileName.setCellValueFactory(new PropertyValueFactory<FileView, String>("fileName"));
        cloudFileSize.setCellValueFactory(new PropertyValueFactory<FileView, String>("fileSize"));
        cloudStorage.setItems(cloudStorageData);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initStorage();

        Network.start();

        Thread t = new Thread(() -> {
            try {
                while (true) {

                    AbstractMessage am = Network.readObject();
                    if(am instanceof AuthResult){
                        AuthResult ay = ((AuthResult) am);
                        if(ay.getResult().equals("ok")){
                            this.login = loginField.getText();
                            File file = new File(clientWay + login);
                            if(!(file).exists())
                            {
                                file.mkdir();
                            }
                            refreshLocalFilesList();
                            setAuthorized();
                        }
                    }
                    if (am instanceof FileMessage) {
                        FileMessage fm = (FileMessage) am;
                        setVisibleLoadPanel(true);
                        boolean append = true;
                        if (fm.partNumber == 1) {
                            append = false;
                        }
                        System.out.println(fm.partNumber + " / " + fm.partsCount);
                        FileOutputStream fos = new FileOutputStream(clientWay + login + "/" + fm.filename, append);
                        fos.write(fm.data);
                        fos.close();
                        double progress = (double) fm.partNumber / (double) fm.partsCount;

                        loadBarProgress.progressProperty().setValue(progress);

                        if (fm.partsCount == fm.partNumber) {
                            loadBarProgress.progressProperty().setValue(0.0);
                            setVisibleLoadPanel(false);
                        }

                        refreshLocalFilesList();
                    }
                    if (am instanceof FileList) {
                        FileList fl = (FileList) am;
                        cloudStorage.getItems().clear();
                        if (fl.getFileList() != null) {
                            for (FileDescription f : fl.getFileList()) {
                                cloudStorageData.add(new FileView(f.getFileName(), getFileSize(f.getFileSize())));
                            }
                        }
                        if(isEmpty(cloudStorageData)){
                            cloudStorageData.add(new FileView(tableEmpty, ""));
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
        Network.sendMsg(new FileList());
        auth.setVisible(false);
        auth.setManaged(false);
        upperPanel.setVisible(false);
        upperPanel.setManaged(false);
        bottomPanel.setVisible(true);
        bottomPanel.setManaged(true);
    }

    private void refreshLocalFilesList() {
        localStorage.getItems().clear();
        File[] files = new File(clientWay + login).listFiles();
        if (files != null) {
            for (File f : files) {
                localStorageData.add(new FileView(f.getName(), getFileSize(f.length())));
            }
        }
        if(isEmpty(localStorageData)){
            localStorageData.add(new FileView(tableEmpty, ""));
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
        if(isExchangedFiles){
            return;
        }
        refreshLocalFilesList();
    }

    public void pressLocalSendBtn(ActionEvent actionEvent) {
        if(isExchangedFiles){
            return;
        }
        FileView fw = localStorage.getSelectionModel().getSelectedItem();

        if (fw != null) {

            if (!fw.getFileName().equals(tableEmpty)) {
                new Thread(() -> {
                    isExchangedFiles = true;
                    setVisibleLoadPanel(true);

                    File file = new File(clientWay + login + "/" + fw.getFileName());

                    int bufSize = 1024 * 1024 * 10;
                    int partsCount = new Long(file.length() / bufSize).intValue();
                    if (file.length() % bufSize != 0) {
                        partsCount++;
                    }
                    FileMessage fmOut = new FileMessage(fw.getFileName(), -1, partsCount, new byte[bufSize]);
                    FileInputStream in = null;
                    try {
                        in = new FileInputStream(file);
                        for (int i = 0; i < partsCount; i++) {
                            int readedBytes = in.read(fmOut.data);
                            fmOut.partNumber = i + 1;
                            if (readedBytes < bufSize) {
                                fmOut.data = Arrays.copyOfRange(fmOut.data, 0, readedBytes);
                            }
                            Network.sendMsg(fmOut);

                            double progress = (double) fmOut.partNumber / (double) fmOut.partsCount;
                            loadBarProgress.progressProperty().setValue(progress);
                            if (fmOut.partsCount == fmOut.partNumber) {
                                loadBarProgress.progressProperty().setValue(0.0);
                                setVisibleLoadPanel(false);
                                isExchangedFiles = false;
                            }
                        }
                        in.close();

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }).start();
            }
        }
    }

    public void pressLocalDeleteBtn(ActionEvent actionEvent) {
        if(isExchangedFiles){
            return;
        }
        FileView fw = localStorage.getSelectionModel().getSelectedItem();
        if(fw != null) {
            if(!fw.getFileName().equals(tableEmpty)) {
                File file = new File(clientWay + login + "/" + fw.getFileName());
                file.delete();
                refreshLocalFilesList();
            }
        }
    }

    public void pressCloudRefreshBtn(ActionEvent actionEvent){
        if(isExchangedFiles){
            return;
        }
        Network.sendMsg(new FileList());
    }

    public void pressCloudSendBtn(ActionEvent actionEvent) {
        if(isExchangedFiles){
            return;
        }
        FileView fw = cloudStorage.getSelectionModel().getSelectedItem();
        if(fw != null) {
            if(!fw.getFileName().equals("")) {
                Network.sendMsg(new FileRequest(fw.getFileName()));
            }
        }
    }

    public void pressCloudDeleteBtn(ActionEvent actionEvent) {
        if(isExchangedFiles){
            return;
        }
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

    private String getFileSize(long size){
        if(size < 1024){
            return String.valueOf(size) + " Б";
        } else if (size < Math.pow(1024, 2)){
            return String.format("%1$.2f", (double)size / Math.pow(1024, 1)) + " кБ";
        } else if  (size < Math.pow(1024, 3)){
            return String.format("%1$.2f", (double)size / Math.pow(1024, 2)) + " МБ";
        } else if  (size < Math.pow(1024, 4)){
            return String.format("%1$.2f", (double)size / Math.pow(1024, 3)) + " ГБ";
        }
        return null;
    }

    public void pressStopLoadFileBtn(ActionEvent actionEvent) {
    }

    private void setVisibleLoadPanel(boolean state){
        updateUI(()->{
            loadPanel.setVisible(state);
            loadPanel.setManaged(state);
        });

    }
}

