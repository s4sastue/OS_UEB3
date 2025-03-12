package os.ueb3;

import os.ueb3.ZFSLib.ZFSFile;
import os.ueb3.ZFSLib.ZFSUtils;

import java.io.IOException;

import static java.lang.Thread.sleep;




public class Main {

    public static void main(String[] args) throws IOException, InterruptedException {
        ZFSUtils.setMode(ZFSUtils.Mode.COMPLETE_SNAPSHOT);

        var z = ZFSFile.createFile("home", "stuelbsasc", "", "test2.txt");

        z.setContent(z.getContentAsString() + "\nTEST");
        sleep(3000);
        z.writeFile();
    }
}
