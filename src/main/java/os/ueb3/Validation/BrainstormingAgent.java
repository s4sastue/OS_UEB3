package os.ueb3.Validation;

import org.json.JSONArray;
import org.json.JSONObject;
import os.ueb3.ZFSLib.ZFSFile;
import os.ueb3.ZFSLib.ZFSUtils;

import java.time.Instant;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;

public class BrainstormingAgent extends Thread {
    private static int numberOfAgents, numberOfTransaktions;
    private static String poolName, datasetName, filePath, filename;
    private static AtomicInteger numberOfSucsessfulTransactions, numberOfFailedTransactions;


    private String name;
    BrainstormingAgent(String name){
        this.name = name;
    }

    JSONObject createNewIdea(String idea, String ideaDescription) {
        JSONObject ideaJSON = new JSONObject();

        ideaJSON.put("idea", idea);
        ideaJSON.put("description", ideaDescription);
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
        for(int i=0; i<numberOfTransaktions; i++){
            try{
                var ideaFile = ZFSFile.readFile(poolName, datasetName, filePath, filename);

                JSONObject ideaObject = new JSONObject(ideaFile.getContentAsString());

                ideaObject.getJSONArray("comments").put(createNewComment("some useful comment from " + name));

                ideaFile.setContent(ideaObject.toString());
                boolean wasWriteSucsessful = ideaFile.writeFile();

                if(wasWriteSucsessful){
                    numberOfSucsessfulTransactions.incrementAndGet();
                }else{
                    numberOfFailedTransactions.incrementAndGet();
                }

            }catch (Exception ignore){}
        }


    }


    static void createDummyIdea(){
        var dummyAgent = new BrainstormingAgent("dummy");
        dummyAgent.createNewIdea("idea", "really detailed description about the new brilliant idea");
    }

    public static void main(String[] args) throws InterruptedException {
        if(args.length != 7){
            System.out.println("ERROR: Keine Argumente spezifiziert.");
            return;
        }

        numberOfAgents = Integer.parseInt(args[0]);
        numberOfTransaktions = Integer.parseInt(args[1]);
        poolName = args[2];
        datasetName = args[3];
        filePath = args[4];
        filename = args[5];

        String mode = args[6];
        switch(mode){
            case "COMPLETE_SNAPSHOT":
                ZFSUtils.setMode(ZFSUtils.Mode.COMPLETE_SNAPSHOT);
                break;

            case "SINGLE_FILE":
                ZFSUtils.setMode(ZFSUtils.Mode.SINGLE_FILE);
                break;

            case "NO_ROLLBACK":
            default:
                ZFSUtils.setMode(ZFSUtils.Mode.NO_ROLLBACK);
        }

        numberOfSucsessfulTransactions = new AtomicInteger(0);
        numberOfFailedTransactions = new AtomicInteger(0);

        long startTime = System.currentTimeMillis();
        var agents = new BrainstormingAgent[numberOfAgents];
        for(int i = 0; i < numberOfAgents; i++){
            agents[i] = new BrainstormingAgent("Agent" + i);
            agents[i].start();
        }

        for(int i = 0; i < numberOfAgents; i++){
            agents[i].join();
        }

        long endTime = System.currentTimeMillis();

        System.out.println("erfolgreiche Transaktionen" + numberOfSucsessfulTransactions.get());
        System.out.println("fehlgeschlagene Transaktionen" + numberOfFailedTransactions.get());
        System.out.println("Dauer (ms)" + (endTime - startTime));
    }
}
