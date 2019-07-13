import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;

public class FileView {

    private final SimpleStringProperty fileName;
    private final SimpleStringProperty fileSize;

    public FileView(String fileName, String fileSize) {
        this.fileName = new SimpleStringProperty(fileName);
        this.fileSize = new SimpleStringProperty(fileSize);
    }

    public String getFileName() {
        return fileName.get();
    }

    public SimpleStringProperty fileNameProperty() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName.set(fileName);
    }

    public String getFileSize() {
        return fileSize.get();
    }

    public SimpleStringProperty fileSizeProperty() {
        return fileSize;
    }

    public void setFileSize(String fileSize) {
        this.fileSize.set(fileSize);
    }
}

