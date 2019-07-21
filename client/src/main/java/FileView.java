import javafx.beans.property.SimpleStringProperty;

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

    public String getFileSize() {
        return fileSize.get();
    }

    public SimpleStringProperty fileSizeProperty() {
        return fileSize;
    }
}

