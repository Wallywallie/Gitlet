package gitlet;

// TODO: any imports you need here
import java.io.File;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.*;

import static gitlet.Repository.REFHEADS_DIR;
import static gitlet.Utils.*;
import static gitlet.Utils.join;

/** Represents a gitlet commit object.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Commit implements Serializable {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Commit class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided one example for `message`.
     */

    /** The message of this Commit. */
    public String message;


    /** The date of the commit */
    private Date timeStamp;

    /** The sha1 code for current commit */
    public String sha1code;

    /** The parent commit for current commit, store the parent's commit in the form of String, namely sha1 code*/
    public String parent;
    public String parent2;

    /** Store the head of current branch*/
    public static String HEAD;
    /* tracked files
    * should map the sha1 code of current commit to the file
    * each file has a sha1 code
    * */
    public List<String> blobs;

    //filename ->its sha1 code
    public TreeMap<String, String> mapping;




    public Commit(String msg) {
        message = msg;
        timeStamp = new Date();

        mapping = new TreeMap<>();
        File masterfile = join(REFHEADS_DIR, "master");
        if (!readContentsAsString(masterfile).isEmpty()) {
            parent = readContentsAsString(masterfile);
        } else {
            parent = "null";
        }


    }

    /** save commits in the name of sha1 code */
    public void saveCommit() {
        File outFile = new File(".gitlet/Commit");
        Utils.writeObject(outFile, this);
        File[] files = Repository.GITLET_DIR.listFiles();
        for (File f : files) {
            if (f.isFile() && f.getName().equals("Commit")) {
                sha1code = Utils.sha1(readContents(f));
                //System.out.println(sha1code);
                String foldername = sha1code.substring(0,2);
                File folder = join(Repository.COMMIT_DIR, foldername);
                if (!folder.exists()) {
                    folder.mkdir();
                }

                //System.out.println(newFilename);
                File newFile = join(folder, sha1code.substring(2));
                f.renameTo(newFile);
            }
        }

        //write current sha1 into branch
        String branch = getCurrBranch();
        File branchfile = join(REFHEADS_DIR, branch);
        writeContents(branchfile, sha1code);

    }

    public String printDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE MMM dd HH:mm:ss yyyy Z", Locale.ENGLISH);
        String output = sdf.format(timeStamp);
        return output;

    }

    public static Commit getBranchCommit(String branch) {
        Commit cmt = null;
        File f = join(REFHEADS_DIR, branch);
        if (f.exists()) {
            String sha1 = readContentsAsString(f);
            cmt = fromFile(sha1);
        }
        return cmt;
    }

    public boolean isTracked(String filename) {
        if (mapping != null) {
            return mapping.containsKey(filename);
        }
        return false;

    }

    public void trackFile(TreeMap<String, String> mp) {
        if (mp != null) {
            for (String key : mp.keySet()) {
                mapping.put(key,  mp.get(key));
            }
        }

    }

    public void untrackFile(TreeMap<String, String> removal) {
        if (removal != null) {
            for (String key : removal.keySet()) {
                mapping.remove(key);
            }
        }
    }
    public static Commit getCurrCommit() {
        Commit curr = null;
        String branch = Commit.getCurrBranch();
        File[] files = REFHEADS_DIR.listFiles();
        String sha1forCurrCommit;
        for (File f : files) {
            if (f.isFile() && f.getName().equals(branch)) {
                sha1forCurrCommit = readContentsAsString(f);
                curr = fromFile(sha1forCurrCommit);
            }
        }
        return curr;
    }

    public static String getCurrBranch() {
        File[] files = Repository.GITLET_DIR.listFiles();
        File HEAD = null;
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && f.getName().equals("HEAD")) {
                    HEAD = f;
                }
            }
        }

        String branch = readContentsAsString(HEAD);
        return branch;
    }
    public static Commit fromFile(String sha1) {
        String foldername = sha1.substring(0, 2);
        File searchingDir = join(Repository.COMMIT_DIR, foldername);

        Commit commit = null;
        if (searchingDir.exists()) {
            File[] files = searchingDir.listFiles();
            for (File f : files) {
                if (f.isFile() && f.getName().equals(sha1.substring(2, sha1.length()))) {
                    commit = readObject(f, Commit.class);
                }
            }
        }

        return commit;
    }

    public static String getBranchSha1(String branchanme) {

        File f = join(REFHEADS_DIR, branchanme);
        if (f.exists()) {
            return readContentsAsString(f);
        }
        return null;

    }


}
