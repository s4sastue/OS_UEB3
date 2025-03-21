package os.ueb3.ZFSLib;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.util.HashMap;
import java.util.concurrent.Semaphore;

public class ZFSFile {
    private static HashMap<String, Semaphore> locks = new HashMap<>();
    private static Semaphore semaphore = new Semaphore(1);

    private Path path;
    private String poolName, datasetName, filePath, filename;

    private FileTime lastModifiedTimeAtReading;
    private ByteBuffer content;

    private ZFSFile(String poolName, String datasetName, String filePath, String filename){
        this.poolName  = poolName ;
        this.datasetName = datasetName;
        this.filePath = filePath;
        this.filename = filename;

        path = Paths.get("/" + poolName + "/" + datasetName + "/" + filePath + "/" + filename);
    }

    public static ZFSFile createFile(String poolName, String datasetName, String filePath, String filename){
        return new ZFSFile(poolName, datasetName, filePath, filename);
    }

    public static ZFSFile readFile(String poolName, String datasetName, String filePath, String filename) throws IOException {
        var file = new ZFSFile(poolName, datasetName, filePath, filename);

        if(!Files.exists(file.path)) {
            return file;
        }

        FileChannel fileChannel = FileChannel.open(file.path, StandardOpenOption.READ);

        file.lastModifiedTimeAtReading = Files.getLastModifiedTime(file.path);
        file.content = ByteBuffer.allocate((int) fileChannel.size());
        fileChannel.read(file.content);
        file.content.flip();

        return file;
    }

    public ByteBuffer getContent(){
        return content;
    }

    public String getContentAsString(){
        return (content == null)
                ? ""
                : StandardCharsets.UTF_8.decode(content.duplicate()).toString();
    }

    public void setContent(ByteBuffer content){
        this.content = content;
    }

    public void setContent(String content){
        this.content = ByteBuffer.wrap(content.getBytes(StandardCharsets.UTF_8));
    }

    public boolean writeFile() throws IOException, InterruptedException {
        boolean doesFileExists = Files.exists(path);

        Semaphore mutex;

        semaphore.acquire();

        if(locks.containsKey(path.toString())) {
            mutex = locks.get(path.toString());
        }else {
            mutex = new Semaphore(1);
            locks.put(path.toString(), mutex);
        }

        semaphore.release();

        mutex.acquire();
        FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock lock = fileChannel.lock();

        String snapshotName = filename + "_" + Instant.now().toString();
        ZFSUtils.createZFSSnapshot(poolName, datasetName, snapshotName);

        FileTime lastModifiedTime = Files.getLastModifiedTime(path);

        fileChannel.write(content);

        if(lastModifiedTimeAtReading != null && lastModifiedTimeAtReading.equals(lastModifiedTime)
                || !doesFileExists && lastModifiedTimeAtReading == null
        ){
            lock.release();
            mutex.release();
            return true;
        }
        else{
            ZFSUtils.rollbackZFSSnapshot(poolName, datasetName, snapshotName, filePath,  filename);
            lock.release();
            mutex.release();
            return false;
        }
    }

    public boolean deleteFile() throws IOException, InterruptedException {
        boolean doesFileExists = Files.exists(path);

        if(!doesFileExists){
            return true;
        }

        FileChannel fileChannel = FileChannel.open(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE);
        FileLock lock = fileChannel.lock();

        String snapshotName = filename + "_" + Instant.now().toString();
        ZFSUtils.createZFSSnapshot(poolName, datasetName, snapshotName);

        FileTime lastModifiedTime = Files.getLastModifiedTime(path);

        Files.delete(path);

        if(lastModifiedTimeAtReading != null && lastModifiedTimeAtReading.equals(lastModifiedTime)){
            lock.release();
            return true;
        }
        else{
            ZFSUtils.rollbackZFSSnapshot(poolName, datasetName, snapshotName, filePath,  filename);
            lock.release();
            return false;
        }
    }
}