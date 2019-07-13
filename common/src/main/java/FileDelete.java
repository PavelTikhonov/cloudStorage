public class FileDelete extends AbstractMessage {

    private String filename;

    public FileDelete(String filename) {
        this.filename = filename;
    }

    public String getFilename() {
        return filename;
    }
}
