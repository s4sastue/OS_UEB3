package os.ueb3.Brainstorming;

import org.json.JSONArray;
import org.json.JSONObject;
import os.ueb3.ZFSLib.ZFSFile;
import os.ueb3.ZFSLib.ZFSUtils;

import java.io.IOException;
import java.time.Instant;
import java.util.Random;

class BrainstormingAgent extends Thread {
    String name;

    BrainstormingAgent(String name){
        this.name = name;
    }

    JSONObject createNewIdea(String idea) {
        JSONObject ideaJSON = new JSONObject();

        ideaJSON.put("idea", idea);
        ideaJSON.put("description", "really detailed description about the new brilliant " + idea);
        ideaJSON.put("author", name);
        ideaJSON.put("timestamp", Instant.now().toString());
        ideaJSON.put("comments", new JSONArray());

        return ideaJSON;
    }

    JSONObject createNewComment(String comment) {
        JSONObject commentJSON = new JSONObject();

        commentJSON.put("comment", comment);
        commentJSON.put("author", name);
        commentJSON.put("timestamp", Instant.now().toString());

        return commentJSON;
    }

    void thinkAboutIdea(JSONObject ideaObject) {
        try{
            sleep(new Random().nextInt(5000-3000) + 3000);
        }catch (InterruptedException ignore){}
    }

    public void run() {
        try{
            var ideaFile = ZFSFile.readFile("osUeb3Pool", "Brainstorming", "", "hypeIdea.txt");

            JSONObject ideaObject = new JSONObject(ideaFile.getContentAsString());
            // think about Idea
            thinkAboutIdea(ideaObject);

            // make some comment
            ideaObject.getJSONArray("comments").put(createNewComment("some useful comment from " + name));

            ideaFile.setContent(ideaObject.toString());
            ideaFile.writeFile();
        }catch (Exception ignore){}
    }

}

public class Brainstorming {
    static void createDummyIdea() throws IOException, InterruptedException {
        BrainstormingAgent dummy = new BrainstormingAgent("dummy");

        var ideaFile = ZFSFile.createFile("osUeb3Pool", "Brainstorming", "", "hypeIdea.txt");

        ideaFile.setContent(dummy.createNewIdea("hype idea").toString());
        ideaFile.writeFile();
    }

    public static void main(String[] args) throws IOException, InterruptedException {
        ZFSUtils.setMode(ZFSUtils.Mode.COMPLETE_SNAPSHOT);
        createDummyIdea();

        BrainstormingAgent agentA = new BrainstormingAgent("agentA"),
            agentB = new BrainstormingAgent("agentB");

        agentA.start();
        agentB.start();

        agentA.join();
        agentB.join();
    }
}
