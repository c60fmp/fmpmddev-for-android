package jp.fmp.c60.fmpmddev;

import java.io.Serializable;


public class ExtDirItem implements Serializable {
    private static final long serialVersionUID = 1L;
    private String extension;
    private String directory;

    public ExtDirItem(String extension, String directory) {
        this.extension = extension;
        this.directory = directory;
    }

    public void setExtension(String extension){
        this.extension = extension;
    }

    public String getExtension(){
        return extension;
    }

    public void setDirectory(String directory){
        this.directory = directory;
    }

    public String getDirectory(){
        return directory;
    }
}
