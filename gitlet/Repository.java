package gitlet;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static gitlet.Utils.*;

// TODO: any imports you need here

/** Represents a gitlet repository.
 *  TODO: It's a good idea to give a description here of what else this Class
 *  does at a high level.
 *
 *  @author TODO
 */
public class Repository {
    /**
     * TODO: add instance variables here.
     *
     * List all instance variables of the Repository class here with a useful
     * comment above them describing what that variable represents and how that
     * variable is used. We've provided two examples for you.
     */

    /** The current working directory. */
    public static final File CWD = new File(System.getProperty("user.dir"));
    /** The .gitlet directory. */
    public static final File GITLET_DIR = join(CWD, ".gitlet");

    public static final File COMMIT_DIR = join(GITLET_DIR, "objects");



    public static final File REF_DIR = join(GITLET_DIR, "refs");

    public static final File REFHEADS_DIR = join(REF_DIR, "heads");
    public static File master; //to record the head of current branch


    /* TODO: fill in the rest of this class. */

    /* ------------These methods handle the "init" command --------------------------- */

    /* This method checks whether GITLET_DIR exists, if exists, print error message and abort */
    public static void checkfolder() {
      if (GITLET_DIR.exists()) {
          System.out.println("A Gitlet version-control system already exists in the current directory.");
          System.exit(0);
      }
    }


    /* This method
    * check failure case
    * create folder and file :  .gitlet/
    *                           .gitlet/HEAD
    *                           .gitlet/objects/
    *                           .gitlet/refs/heads/
    * creates an initial commit, when initializing a commit should:
    *           ->timeStamp
    *           ->log message
    *           ->save information to system
    * creates staging area
    *
    * */

    public static void initialization() {
        checkfolder();
        GITLET_DIR.mkdir();
        File HEAD = join(GITLET_DIR, "HEAD");
        writeContents(HEAD,"master"); //初始化HEAD文件

        COMMIT_DIR.mkdir(); //------> not check yet
        REF_DIR.mkdir();
        REFHEADS_DIR.mkdir();
        master =  join(REFHEADS_DIR, "master");
        try {
            master.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }

        //initial commit
        String MSG = "initial commit";
        Commit initialCommit = new Commit(MSG);
        initialCommit.saveCommit();

        //initialize staging area
        Index index = new Index();
        index.saveIndex();

    }


    /* ------------These methods handle the "add" command --------------------------- */

    public static void add(String filename) {
        /* if the current working version of the file is identical to the version in the current commit,
        * do not stage it to be added,and remove it from the staging area if it is already there
        * */

        Index index;
        File fileToAdd = null;
        Commit curr;
        //find file to be added
        File[] files = CWD.listFiles();
        for (File file : files) {
            if (file.isFile() && filename.equals(file.getName())) {
                fileToAdd = file;
            }
        }
        //check if the file exists
        if (fileToAdd == null) {
            System.out.println("File does not exist.");
            System.exit(0);
        }

        //create a file blob
        Blob blob = new Blob(fileToAdd);


        //check if the file is equal to current commit version
        //curr version->head
        //is equal->tracked file
        index = Index.fromFile();
        curr = Commit.getCurrCommit();
        if (curr.mapping.containsKey(blob.filename) && curr.mapping.get(blob.filename).equals(blob.sha1)) {
            if (index.isTracked(blob.filename)) {
                index.untrackFile(blob.filename);
            }
            return;
        }

        //write file name and its corresponding sha1 to index
        index.trackFile(filename, blob.sha1);
        index.saveIndex();

        //save file to repo in the name of sha1
        blob.saveBlob();
    }

    /* ------------These methods handle the "commit" command --------------------------- */
    public static void gitCommit(String msg) {

        //failure cases: no comment
        if (msg.isEmpty()) {
            System.out.println("Please enter a commit message.");
            System.exit(0);
        }

        //failure cases: no file has been staged
        Index index = Index.fromFile();
        //System.out.println(index.getTrackedFile().toString());
        TreeMap<String, String> mapping = index.getTrackedFile();
        TreeMap<String, String> removal = index.getRemovalFile();
        if (mapping == null) {
            System.out.println("No changes added to the commit.");
            System.exit(0);
        }


        Commit curr = new Commit(msg);

        //record the parent's recordings into current commit
        Commit parentCommit = null;
        if (curr.parent != null) {
            // to find the exact commit using sha1 code of current commit
            parentCommit = Commit.fromFile( curr.parent);
        }
        if (parentCommit != null) {
            curr.trackFile(parentCommit.mapping);
        }

        //record the staging area into commit;
        curr.untrackFile(removal);
        curr.trackFile(mapping);


        curr.saveCommit();

        //clean the staging area
        Index.cleanStaging();

    }

    /* ------------These methods handle the "remove" command --------------------------- */
    public static void remove(String filename) {
        //failure case: if file is not staged or tracked by current commit.
        Commit curr = Commit.getCurrCommit();
        Index idx = Index.fromFile();
        boolean isStaged = idx.isTracked(filename);
        boolean isTracked = curr.isTracked(filename);
        if (!(isStaged || isTracked)) {
            System.out.println("No reason to remove the file.");
            return;
        }


        if (isStaged) {
            //delete the relevant blob
            String sha1 = idx.getTrackedFile().get(filename);
            File f = join(COMMIT_DIR, sha1.substring(0, 2));
            f = join(f, sha1.substring(2, sha1.length()));
            if (f.exists()) {
                f.delete();
            }

            //untrack
            idx.untrackFile(filename);
        }

        if (isTracked) {


            //delete the relevant blob  ------>to resolve: should i delete relevant blob?
            String sha1 = curr.mapping.get(filename);
            /*
            File f = join(COMMIT_DIR, sha1.substring(0, 2));
            f = join(f, sha1.substring(2, sha1.length()));
            if (f.exists()) {
                f.delete();
            }
            */


            //stage for removal
            idx.trackFileToRemove(filename, sha1);

            //delete file in working directory
            File fInWorkingDir = join(CWD, filename);
            if (fInWorkingDir.exists()) {
                restrictedDelete(fInWorkingDir);
            }

        }
        idx.saveIndex();
    }

    /* ------------These methods handle the "log" command --------------------------- */
    public static void log() {
        Commit curr = Commit.getCurrCommit();

        String currBranch = Commit.getCurrBranch();
        File f = join(REFHEADS_DIR, currBranch);
        String sha1 = null;
        if (f.exists()) {
            sha1 = readContentsAsString(f);
        }

        printLog(curr, sha1);

    }

    public static void globalLog() {
        //TODO: re-print a lot of common commits

        //iterate through the head dir and get a list of sha1 String
        List<String> lst = plainFilenamesIn(REFHEADS_DIR);
        if (lst != null) {
            for (String i : lst) {
                File f = join(REFHEADS_DIR, i);
                String sha1 = null;
                Commit curr = null;
                if (f.exists()) {
                    sha1 = readContentsAsString(f);
                    curr = Commit.fromFile(sha1);
                }
                printLog( curr,sha1);
            }
        }


    }

    private static void printLog(Commit curr, String sha1) {
        while (curr != null) {
            System.out.println("===");
            System.out.println("commit " + sha1);
            //case: merge commit
            if (curr.parent2 != null) {
                System.out.println("Merge: " + curr.parent.substring(0, 7) + " " + curr.parent2.substring(0, 7));
            }
            System.out.println("Date: " + curr.printDate());
            System.out.println(curr.message);
            System.out.println();

            String parentCommit = curr.parent;
            curr = Commit.fromFile(parentCommit);
            sha1 = parentCommit;

        }

    }

    /* ------------These methods handle the "find" command --------------------------- */
    public static void find(String msg) {
        List<String> lst = plainFilenamesIn(REFHEADS_DIR);
        boolean isFind = false;
        if (lst != null) {
            String sha1;
            Commit curr;

            for (String i : lst) {
                File f = join(REFHEADS_DIR, i);

                if (f.exists()) {
                    sha1 = readContentsAsString(f);
                    curr = Commit.fromFile(sha1);

                    while(curr != null) {
                        String currMsg = curr.message;
                        if (currMsg.equals(msg)) {
                            System.out.println(sha1);
                            isFind = true;
                            break;
                        }
                        sha1 = curr.parent;
                        curr = Commit.fromFile(sha1);
                    }
                }
            }
        }

        if (!isFind) {
            System.out.println("Found no commit with that message.");
        }
    }

    /* ------------These methods handle the "status" command --------------------------- */
    public static void status() {
        //branches
        System.out.println("=== Branches ===");
        List<String> lst = plainFilenamesIn(REFHEADS_DIR);

        File Head = join(GITLET_DIR, "HEAD");
        String head = readContentsAsString(Head);

        if (lst != null) {
            Collections.sort(lst);
            for (String i : lst) {
                if (head.equals(i)) {
                    System.out.print("*");
                }
                System.out.println(i);
            }
        }
        System.out.println();

        //staged files for addition or removed files
        Index idx = Index.fromFile();
        TreeMap<String, String> addition = idx.getTrackedFile();
        TreeMap<String, String> removal = idx.getRemovalFile();

        System.out.println("=== Staged Files ===");
        if (addition != null) {
            for (String k : addition.keySet()) {
                System.out.println(k);
            }
        }
        System.out.println();

        System.out.println("=== Removed Files ===");
        if (removal != null) {
            for (String k : removal.keySet()) {
                System.out.println(k);
            }
        }
        System.out.println();

        //TODO:Modifications Not Staged For Commit
        System.out.println("=== Modifications Not Staged For Commit ===");
        System.out.println();


        System.out.println("=== Untracked Files ===");
        String currBranch = Commit.getCurrBranch();
        File[] files = CWD.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && !isWorkingFileTracked(f.getName(), currBranch)) {
                    System.out.println(f.getName());
                }
            }
        }
        System.out.println();

    }

    /* ------------These methods handle the "checkout" command --------------------------- */
    public static void checkoutBranch(String branch) {
        //failure case: no such branch exists
        List<String> lst = plainFilenamesIn(REFHEADS_DIR);
        if (lst != null) {
            if (!lst.contains(branch)) {
                System.out.println("No such branch exists.");
                return;
            }
        }

        //failure case: if branch is the current branch
        String currBranchName = Commit.getCurrBranch();
        if (branch.equals(currBranchName)) {
            System.out.println("No need to checkout the current branch.");
            return;
        }

        //failure case: a working file is untracked in the current branch
        checkUntrackedFile();

        //take all relevant files and put them in CWD
        Commit cmt = Commit.getBranchCommit(branch);
        if (cmt != null) {
            if (!cmt.mapping.isEmpty()) {
                for (String i : cmt.mapping.keySet()) {
                    overwrite(cmt.mapping.get(i));
                }

                //files tracked in the current branch but not in the check-out branch should be deleted
                Commit curr = Commit.getCurrCommit();
                if (!curr.mapping.isEmpty()) {
                    for (String i : curr.mapping.keySet()) {
                        if (!cmt.mapping.containsKey(i)) {
                            File f = join(CWD, i);
                            if (f.exists()) {
                                f.delete();
                            }
                        }
                    }
                }
            }
        }

        //the check-out branch is considered as the current branch
        File Head = join(GITLET_DIR, "HEAD");
        if (Head.exists()) {
            writeContents(Head, branch);
        }

        //clean the staging area
        Index.cleanStaging();
    }

    public static void checkoutCfile(String commitId, String filename) {

        //failure case: commitId does not exist
        Commit cmt = checkId(commitId);
        if ( cmt == null) {
            return;
        }

        checkout(cmt, filename);
    }

    public static void checkoutFile(String filename) {

        Commit curr = Commit.getCurrCommit();
        checkout(curr, filename);

    }

    private static Commit checkId(String commitId){

        //failure case: commitId does not exist
        List<String> lst = plainFilenamesIn(REFHEADS_DIR);
        String sha1;
        Commit curr;
        if (lst != null) {


            for (String i : lst) {
                File f = join(REFHEADS_DIR, i);

                if (f.exists()) {
                    sha1 = readContentsAsString(f);
                    curr = Commit.fromFile(sha1);

                    while(curr != null) {
                        //case: abbreviate hexadecimal commitId
                        if (commitId.equals(sha1) || commitId.equals(sha1.substring(0, 6))) {
                            return curr;
                        }
                        String parentCommit = curr.parent;
                        curr = Commit.fromFile(parentCommit);
                        sha1 = parentCommit;
                    }
                }
            }
        }
        System.out.println("No commit with that id exists.");
        return null;
    }

    private static boolean isWorkingFileTracked(String name, String branch) {
        //untracked files: files present in the working directory but neither staged for addition nor tracked.
        boolean isStaged;
        boolean isInRemoval;
        boolean isTracked;

        //file not staged
        Index idx = Index.fromFile();

        isStaged = idx.isTracked(name);
        isInRemoval = idx.isInRemoval(name);

        //file not tracked in current branch commit
        Commit cmt = Commit.getBranchCommit(branch);
        isTracked = cmt.isTracked(name);

        if (((!isStaged) && (!isTracked)) || isInRemoval) {
            return false;
        }
        return true;
    }

    private static void checkout(Commit cmt, String filename) {
        //failure case:the file does not exit
        if (!cmt.isTracked(filename)) {
            System.out.println("File does not exist in that commit.");
            System.exit(0);
        }

        //overwrite the file
        overwrite(cmt.mapping.get(filename));
    }

    private static void overwrite(String sha1) {
        Blob blob = Blob.fromFile(sha1);
        if (blob != null) {
            File f = new File(blob.filename);
            blob.writeInFile(f);
        }
    }
    private static void checkUntrackedFile() {
        String currBranch = Commit.getCurrBranch();
        File[] files = CWD.listFiles();
        if (files != null) {
            for (File f : files) {
                if (f.isFile() && !isWorkingFileTracked(f.getName(), currBranch)) {
                    System.out.println("There is an untracked file in the way; delete it, or add and commit it first.");
                    System.exit(0);
                }
            }
        }

    }


    /* ------------These methods handle the "branch" command --------------------------- */
    public static void createBranch(String branch) {
        //failure case: if the branch already exists
        List<String> lst = plainFilenamesIn(REFHEADS_DIR);
        if (lst != null) {
            for (String i : lst) {
                if (i.equals(branch)) {
                    System.out.println("A branch with that name already exists.");
                    return;
                }
            }
        }
        File newBranch = join(REFHEADS_DIR, branch);
        try {
            newBranch.createNewFile();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (newBranch.exists()) {
            String currBranchName = Commit.getCurrBranch();
            File currBranch = join(REFHEADS_DIR, currBranchName);
            if (currBranch.exists()) {
                String currSha1 = readContentsAsString(currBranch);
                writeContents(newBranch, currSha1);

            }
        }

    }

    /* ------------These methods handle the "rm-branch" command --------------------------- */
    public static void removeBranch(String branch) {
        //failure case: if the branch does not exist
        List<String> lst = plainFilenamesIn(REFHEADS_DIR);
        if (lst != null) {
            if (!lst.contains(branch)) {
                System.out.println("A branch with that name does not exist.");
                return;
            }
        }

        //failure case: can't remove current branch
        String currBranch = Commit.getCurrBranch();
        if (currBranch.equals(branch)) {
            System.out.println("Cannot remove the current branch.");
            return;
        }

        //remove pointer associated with the branch and do not delete commit
        File f = join(REFHEADS_DIR, branch);
        if (f.exists()) {
            f.delete();
        }



    }

    /* ------------These methods handle the "reset" command --------------------------- */

    public static void reset(String commitId) {
        //TODO: testing
        //TODO: simplify the code with part of "checkout branch"

        //failure case: commitId does not exist
        Commit cmt = checkId(commitId);
        if (cmt == null) {
            return;
        }

        //failure case: a working file is untracked in the current branch
        checkUntrackedFile();

        //Checks out all the files tracked by the given commit.
        if (!cmt.mapping.isEmpty()) {
            for (String i : cmt.mapping.keySet()) {
                overwrite(cmt.mapping.get(i));
            }

            //Removes tracked files that are not present in that commit.
            Commit curr = Commit.getCurrCommit();
            if (curr != null) {
                if (!curr.mapping.isEmpty()) {
                    for (String k : curr.mapping.keySet()) {
                        //if cmt does not track file tracked by curr, delete the file
                        if (!cmt.mapping.containsKey(k)) {
                            File f = join(CWD, k);
                            if (f.exists()) {
                                f.delete();
                            }
                        }
                    }
                }

            }

        }

        //moves the current branch’s head to that commit node.
        String branch = Commit.getCurrBranch();
        File f = join(REFHEADS_DIR, branch);
        if (f.exists()) {
            writeContents(f, commitId);
        }

        //clean staging area
        Index.cleanStaging();
    }

    /* ------------These methods handle the "merge" command --------------------------- */
    public static void merge(String branchname) {
        //TODO: if branch does not exist

        //find the split commit
        List<String> commitIds = new ArrayList<>();
        Commit curr = Commit.getCurrCommit();
        String currbranch = Commit.getCurrBranch();
        String sha1 = Commit.getBranchSha1(currbranch);

        Commit branch = Commit.getBranchCommit(branchname);
        String givenbranchId = Commit.getBranchSha1(branchname);
        String bsha1 = givenbranchId;

        String commonId = null;

        TreeMap<String, String> currMp = curr.mapping;
        TreeMap<String, String> branchMp = branch.mapping;

        while (curr != null) {
            commitIds.add(sha1);

            sha1 = curr.parent;
            curr = Commit.fromFile(sha1);
        }

        while (branch != null){
            if (commitIds.contains(bsha1)) {
                commonId = bsha1;
                break;
            }
            bsha1 = branch.parent;
            branch = Commit.fromFile(bsha1);
        }

        //If the split point is the same commit as the given branch
        if (givenbranchId.equals(commonId)) {
            System.out.println("Given branch is an ancestor of the current branch.");
            return;
        }

        //If the split point is the current branch, then the effect is to check out the given branch
        if (currbranch.equals(commonId)) {
            checkoutBranch(branchname);
            System.out.println("Current branch fast-forwarded.");
            return;
        }

        //Any files that have been modified in the given branch since the split point, but not modified in the current branch since the split point should be changed to their versions in the given branch

        Commit common = Commit.fromFile(commonId);
        TreeMap<String, String> commonMp = common.mapping;

        for (String i : branchMp.keySet()) {
            if ((!commonMp.containsKey(i) || (!branchMp.get(i).equals(commonMp.get(i))))) {
                overwrite(branchMp.get(i));

                //then automatically staged
                add(i);
            }

        }


        Set<String> files = new HashSet<>();

        files.addAll(currMp.keySet());
        files.addAll(branchMp.keySet());
        files.addAll(commonMp.keySet());

        curr = Commit.getCurrCommit();
        branch = Commit.getBranchCommit(branchname);
        boolean isConflict = false;

        for (String f : files) {
            status b = getStatus(common, branch, f);
            status c = getStatus(common, curr, f);
            if ( (b.equals(status.modified) && c.equals(status.unmodified)) || (b.equals(status.added) && c.equals(status.notExist))) {

                /*1.Any files that have been modified in the given branch since the split point,
                 * but not modified in the current branch since the split point should be changed to their versions in the given branch
                 * (checked out from the commit at the front of the given branch).
                 * These files should then all be automatically staged.
                 * */

                /*5.Any files that were not present at the split point and are present only in the given branch should be checked out and staged.
                 */


                checkout(branch,f);
                add(f);

            } else if (b.equals(status.removed) && c.equals(status.unmodified)) {
                /*6.Any files present at the split point, unmodified in the current branch,
                 * and absent in the given branch should be removed (and untracked).
                 * */
                remove(f);

            } else if (b.equals(status.unmodified) || b.equals(status.notExist) || (b.equals(status.removed) && c.equals(status.removed))) {
                continue;
            } else {
                String currVersion = currMp.get(f);
                String branchVersion = branchMp.get(f);
                if (currVersion != null && currVersion.equals(branchVersion)) {
                    continue;
                }
                handleContents(f, currVersion, branchVersion);
                isConflict = true;



            }
            //merge commit
            String msg = "Merged " + branchname + " into " + currbranch;
            Index idx = Index.fromFile();
            Commit merge = new Commit(msg);

            merge.trackFile(currMp);
            merge.trackFile(branchMp);
            merge.parent2 = branchname;

            //record the staging area into commit;
            merge.untrackFile(idx.getRemovalFile());
            merge.trackFile(idx.getTrackedFile());

            merge.saveCommit();

            //clean the staging area
            Index.cleanStaging();
            if (isConflict) {
                System.out.println("Encountered a merge conflict.");
            }



        }

    }
    private static void handleContents(String name, String currSha1, String branchSha1) {
        StringBuilder builder = new StringBuilder();
        builder.append("<<<<<<< HEAD");
        builder.append("\n");
        if (currSha1 != null) {
            String content = Blob.fromFile(currSha1).getContent();
            builder.append(content);
            builder.append("\n");

        }

        builder.append("=======");
        builder.append("\n");
        if (branchSha1 != null) {
            String content = Blob.fromFile(branchSha1).getContent();
            builder.append(content);
            builder.append("\n");
        }
        builder.append(">>>>>>>");
        File f = join(CWD, name);

        writeContents(f, builder.toString());

    }

    private enum status{
        unmodified, modified, removed, added, notExist
    }
    private static status getStatus(Commit common, Commit cmt, String filename) {
        TreeMap<String, String> commonMp = common.mapping;
        TreeMap<String, String> cmtMp = cmt.mapping;
        if (commonMp.containsKey(filename) && cmtMp.containsKey(filename)) {

            if (commonMp.get(filename).equals(cmtMp.get(filename))  ) {
                return status.unmodified;
            } else {
                return status.modified;
            }
        } else if (commonMp.containsKey(filename)) {
            return status.removed;

        } else if (cmtMp.containsKey(filename)) {
            return status.added;
        } else {
            return status.notExist;
        }

    }


}
