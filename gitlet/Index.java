package gitlet;

import jdk.jshell.execution.Util;

import java.io.File;
import java.io.Serializable;
import java.util.HashMap;
import java.util.TreeMap;

public class Index implements Serializable {


    private TreeMap<String, String> mapping; //using mapping to record which file to be tracked

    private TreeMap<String, String> removal; // used for record file to be removed

    private class CommitTree {

        String cur;
        CommitTree prev;
        CommitTree(String c ,CommitTree p) {
            cur = c;
            prev = p;
        }
        CommitTree(String c) {
            cur = c;
            prev = null;
        }

    }

    public CommitTree t;

    public Index() {

        mapping = new TreeMap<>();
        removal = new TreeMap<>();
    }

    public void trackFile(String filename, String sha1) {
        mapping.put(filename, sha1);
    }
    public void untrackFile(String filename) {
        if (mapping.containsKey(filename)) {
            mapping.remove(filename);
        }
    }

    public boolean isTracked(String filename) {
        if (mapping != null) {
            return mapping.containsKey(filename);
        }
        return false;
    }
    public boolean isInRemoval(String filename) {
        if (removal != null) {
            return removal.containsKey(filename);
        }
        return false;
    }
    public TreeMap<String, String> getTrackedFile () {
        return mapping;
    }

    public TreeMap<String, String> getRemovalFile() {return removal;}

    public void trackFileToRemove(String filename, String sha1) {

        removal.put(filename, sha1);
    }


    public void saveIndex() {
        File outfile = Utils.join(Repository.GITLET_DIR, "index");
        Utils.writeObject(outfile, this);
    }

    public static Index fromFile() {
        File[] files = Repository.GITLET_DIR.listFiles();
        Index returnIndex;
        for (File f : files) {
            if (f.isFile() && f.getName().equals("index")) {
                returnIndex = Utils.readObject(f, Index.class);
                return returnIndex;
            }
        }
        return null;
    }
    public static void cleanStaging() {
        //clean the staging area
        Index newIndex = new Index();
        newIndex.saveIndex();
    }
}
