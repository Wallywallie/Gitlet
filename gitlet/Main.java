package gitlet;
import static gitlet.Repository.*;
import static gitlet.Utils.*;

/** Driver class for Gitlet, a subset of the Git version-control system.
 *  @author TODO
 */
public class Main {

    /** Usage: java gitlet.Main ARGS, where ARGS contains
     *  <COMMAND> <OPERAND1> <OPERAND2> ... 
     */
    public static void main(String[] args) {
        // TODO: what if args is empty?
        if (args.length == 0) {
            System.out.println("Please enter a command.");
            System.exit(0);
        }

        String firstArg = args[0];



        switch(firstArg) {
            case "init":
                initialization();
                break;
            case "add":
                String filename = args[1];
                add(filename);
                break;

            case "commit":
                String msg = args[1];//---->unchecked
                gitCommit(msg);
                break;

            case "rm":
                String fname = args[1];
                remove(fname);
                break;
            case "log":
                log();
                break;
            case "global-log":
                globalLog();
                break;
            case "find":
                String mg = args[1];
                find(mg);
                break;
            case "status":
                status();
                break;
            case "checkout":
                int cnt = args.length;
                if (cnt == 2) {
                    String brachName = args[1];
                    checkoutBranch(brachName);
                    break;
                } else if (cnt == 3){
                    String fileName = args[2];
                    checkoutFile(fileName );
                    break;
                } else if (cnt == 4) {
                    String commitId = args[1];
                    String fileName = args[3];
                    checkoutCfile(commitId, fileName);
                    break;
                }
                break;
            case "branch":
                String branch = args[1];
                createBranch(branch);
                break;
            case "rm-branch":
                String branchname = args[1];
                removeBranch(branchname);
                break;
            case "reset":
                String commitId = args[1];
                reset(commitId);
                break;
            case "merge":
                String branchName = args[1];
                merge(branchName);
        }


    }
}
