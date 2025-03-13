package os.ueb3.ZFSLib;

import java.io.IOException;

public class ZFSUtils {
    public enum Mode {
        COMPLETE_SNAPSHOT,
        SINGLE_FILE
    }

    private static ZFSUtils.Mode mode;

    static void createZFSSnapshot(String poolName, String datasetName, String snapshotName) throws IOException, InterruptedException {
        System.out.println("Creating ZFS snapshot: " + snapshotName);

        ProcessBuilder processBuilder = new ProcessBuilder("sudo", "zfs", "snapshot", poolName + "/" + datasetName + "@" + snapshotName);
        processBuilder.redirectErrorStream(true);

        Process process = processBuilder.start();
        process.waitFor();

    }

    static void rollbackZFSSnapshot(String poolName, String datasetName, String snapshotName, String filePath, String filename) throws IOException, InterruptedException{
        ProcessBuilder processBuilder;
        Process process;

        switch (mode){
            case SINGLE_FILE:
                System.out.println("Rolling back single file (" + filePath + "/" + filename +  ") from snapshot: " + snapshotName);

                processBuilder = new ProcessBuilder("cp",
                        "/" + poolName + "/" + datasetName + "/.zfs/snapshot/" + snapshotName + "/" + filePath + "/" + filename,
                        "/" + poolName + "/" + datasetName + "/" + filePath + "/" + filename
                );
                processBuilder.redirectErrorStream(true);

                process = processBuilder.start();
                process.waitFor();

                break;

            case COMPLETE_SNAPSHOT:
            case null:
            default:
                System.out.println("Rolling back ZFS snapshot: " + snapshotName);

                processBuilder = new ProcessBuilder("sudo", "zfs", "rollback", "-r", poolName + "/" + datasetName + "@" + snapshotName);
                processBuilder.redirectErrorStream(true);

                process = processBuilder.start();
                process.waitFor();
        }
    }

    public static void setMode(ZFSUtils.Mode mode){
        ZFSUtils.mode = mode;
    }
}