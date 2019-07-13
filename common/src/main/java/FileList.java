import java.util.ArrayList;
import java.util.List;

public class FileList extends AbstractMessage {

    private List<FileDescription> fileList;

    public FileList() {
        this.fileList = new ArrayList<>();
    }

    public FileList(List<FileDescription> fileList) {
        this.fileList = fileList;
    }

    public List<FileDescription> getFileList() {
        return fileList;
    }

    public void addFileDescriptionToList(FileDescription fd){
        fileList.add(fd);
    }

}
