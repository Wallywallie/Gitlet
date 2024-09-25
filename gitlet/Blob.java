package gitlet;

import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;

import static gitlet.Repository.COMMIT_DIR;
import static gitlet.Repository.GITLET_DIR;
import static gitlet.Utils.*;

 class Blob implements Serializable {


    public String sha1;

    public String filename;

    private byte[] contents;


    public Blob(File f) {
        contents = Utils.readContents(f);
        filename = f.getName();
        sha1 = Utils.sha1(contents);

    }


    public void saveBlob() {

        File outFile = join(GITLET_DIR, "Blob");
        Utils.writeObject(outFile, this);
        String foldername = sha1.substring(0,2);
        File folder = join(COMMIT_DIR, foldername);
        if (!folder.exists()) {
            folder.mkdir();
        }
        File newFile = join(folder, sha1.substring(2));
        outFile.renameTo(newFile);

    }
    public static Blob fromFile(String sha1) {
        //TODO: failure case sha1
        Blob blob = null;
        File dir = join(COMMIT_DIR, sha1.substring(0, 2));
        if (dir.exists()) {
            File[] files = dir.listFiles();
            if (files != null) {
                for (File f : files) {
                    if (f.getName().equals(sha1.substring(2))) {
                        blob = readObject(f, Blob.class);
                    }
                }
            }
        }
        return blob;
    }

    public void writeInFile(File file) {
        writeContents(file, new String(contents, StandardCharsets.UTF_8));
    }
    public String getContent() {
        return new String(contents, StandardCharsets.UTF_8);
    }

}
