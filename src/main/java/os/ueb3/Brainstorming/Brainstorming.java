package os.ueb3.Brainstorming;

import org.json.JSONArray;
import org.json.JSONObject;
import os.ueb3.ZFSLib.ZFSFile;

import java.io.IOException;
import java.nio.file.*;
import java.time.Instant;
import java.util.HashMap;
import java.util.Scanner;
import java.util.stream.Stream;

public class Brainstorming {

    private static String name;
    private static String poolName, datasetName, filePath;
    private static Path path;

    static HashMap<String, ZFSFile> listIdeas(){
        HashMap<String, ZFSFile> zfsFiles = new HashMap<>();

        try (Stream<Path> files = Files.list(path)){
            String[] fileNames = files.map(Path::getFileName).map(Path::toString).toArray(String[]::new);

            if (fileNames.length == 0) {
                System.out.println("Bisher keine Ideen angelegt");
                return null;
            }

            System.out.println("Auflistung aller gefundenen Ideen");
            String format = "| %-10s | %-20s | %-40s | %-10s | %-25s | %-1s %n";
            System.out.printf(format, "Dateiname", "valide JSON-Datei", "Idee", "Author", "Anzahl der Kommentare", "Beschreibung");
            System.out.println("+------------+----------------------+------------------------------------------+------------+---------------------------+-----------------------------");

            for (String fileName : fileNames) {
                var ideaFile = ZFSFile.readFile(poolName, datasetName, filePath, fileName);

                try {
                    JSONObject ideaJSON = new JSONObject(ideaFile.getContentAsString());

                    System.out.printf(format,
                            fileName,
                            "✓",
                            ideaJSON.getString("idea"),
                            ideaJSON.getString("author"),
                            ideaJSON.getJSONArray("comments").length(),
                            ideaJSON.getString("description")
                    );

                    zfsFiles.put(fileName, ideaFile);
                } catch (Exception ignore) {
                    System.out.printf(format, fileName, "✗", "", "", "", "");
                }
            }

        } catch (IOException ignore) {}

        return zfsFiles;
    }

    static void createNewIdea(Scanner scanner) {

        System.out.print("Bitte geben Sie einen Dateinamen an, in der die Idee als JSON abgespeichert werden soll: ");
        String fileName = scanner.nextLine();

        System.out.print("Bitte geben Sie den Namen der Idee an: ");
        String idea = scanner.nextLine();

        System.out.print("Bitte geben Sie die Beschreibung der Idee an: ");
        String ideaDescription = scanner.nextLine();


        JSONObject ideaJSON = new JSONObject();
        ideaJSON.put("idea", idea);
        ideaJSON.put("description", ideaDescription);
        ideaJSON.put("author", name);
        ideaJSON.put("timestamp", Instant.now().toString());
        ideaJSON.put("comments", new JSONArray());

        var zfsFile = ZFSFile.createFile(poolName, datasetName, filePath, fileName);
        zfsFile.setContent(ideaJSON.toString());
        boolean wasWriteSuccessful = false;
        try{
            wasWriteSuccessful = zfsFile.writeFile();
        }catch (Exception ignore){}

        if(wasWriteSuccessful){
            System.out.println("Idee erfolgreich gespeichert");
        }else{
            System.out.println("Konflikt aufgetreten - Transaktion wurde zurückgesetzt");
        }

    }

    static void createNewComment(Scanner scanner) {
        HashMap<String, ZFSFile> zfsFiles = listIdeas();

        if (zfsFiles == null) {
            return;
        } else if(zfsFiles.isEmpty()) {
            System.out.println("Keine validen Dateien gefunden!");
            return;
        }

        System.out.print("Geben Sie den Dateinamen der Idee an, die Sie kommentieren möchten: ");
        String fileName = scanner.nextLine();

        if (!zfsFiles.containsKey(fileName)){
            System.out.println("Dateiname existiert nicht oder ist keine valide JSON-Datei");
            return;
        }

        var zfsFile = zfsFiles.get(fileName);

        System.out.print("Geben Sie den Kommentar an (Drücken Sie << X >> um abzubrechen): ");
        String comment = scanner.nextLine();

        if(comment.equals("X") || comment.isEmpty()){
            return;
        }

        JSONObject commentJSON = new JSONObject();
        commentJSON.put("comment", comment);
        commentJSON.put("author", name);
        commentJSON.put("timestamp", Instant.now().toString());

        JSONObject ideaJSON = new JSONObject(zfsFile.getContentAsString());
        ideaJSON.getJSONArray("comments").put(commentJSON);

        zfsFile.setContent(ideaJSON.toString());

        boolean wasWriteSuccessful = false;
        try{
            wasWriteSuccessful = zfsFile.writeFile();
        }catch (Exception ignore){}

        if(wasWriteSuccessful){
            System.out.println("Idee erfolgreich gespeichert");
        }else{
            System.out.println("Konflikt aufgetreten - Transaktion wurde zurückgesetzt");
        }
    }

    static void deleteIdea(Scanner scanner) {
        HashMap<String, ZFSFile> zfsFiles = listIdeas();

        if (zfsFiles == null) {
            return;
        } else if(zfsFiles.isEmpty()) {
            System.out.println("Keine validen Dateien gefunden!");
            return;
        }

        System.out.print("Geben Sie den Dateinamen der Idee an, die Sie löschen möchten: ");
        String fileName = scanner.nextLine();

        if (!zfsFiles.containsKey(fileName)){
            System.out.println("Dateiname existiert nicht oder ist keine valide JSON-Datei");
            return;
        }

        var zfsFile = zfsFiles.get(fileName);
        boolean wasWriteSuccessful = false;
        try{
            wasWriteSuccessful = zfsFile.deleteFile();
        }catch (Exception ignore){}

        if(wasWriteSuccessful){
            System.out.println("Idee erfolgreich gelöscht");
        }else{
            System.out.println("Konflikt aufgetreten - Transaktion wurde zurückgesetzt");
        }
    }

    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        if(args.length != 4){
            System.out.println("Keine gültige Programmparameter!");

            System.out.print("Geben Sie ihren Namen an: ");
            name = scanner.nextLine();

            System.out.print("Geben Sie den ZFS-Poolnamen an: ");
            poolName = scanner.nextLine();

            System.out.print("Geben Sie den Namen des ZFS-Datensatzes an: ");
            datasetName = scanner.nextLine();

            System.out.print("Geben Sie den Pfad an, an dem die Ideen verwaltet werden sollen: ");
            filePath = scanner.nextLine();
        }else{
            name = args[0];
            poolName = args[1];
            datasetName = args[2];
            filePath = args[3];
        }

        path = Paths.get("/", poolName, datasetName, filePath);

        while(true) {
            System.out.println("\n\n<------------------------------------------------------------------->");
            System.out.println("<--                          Brainstorming                        -->");
            System.out.println("<------------------------------------------------------------------->");
            System.out.println("<-   Sie befinden sich im Hauptmenü                                ->");
            System.out.println("<-   Funktionen:                                                   ->");
            System.out.println("<------------------------------------------------------------------->");
            System.out.println(" ->                       0 <- Auflistung aller bestehenden Idee");
            System.out.println(" ->                       1 <- Erstellung einer neuen Idee");
            System.out.println(" ->                       2 <- Kommentierung einer bestehenden Idee");
            System.out.println(" ->                       3 <- Löschen einer bestehenden Idee");
            System.out.println(" -> << sonstige Eingaben >> <- Terminierung");
            System.out.println("<------------------------------------------------------------------->");
            System.out.print("<-  Wählen Sie eine Funktion indem Sie die angegebene Taste drücken: ");

            String option = scanner.nextLine();
            System.out.println("\n\n\n\n\n");
            switch (option) {
                case "0":
                    listIdeas();
                    System.out.print("Drücken Sie << Enter >> um fortzufahren: ");
                    scanner.nextLine();
                    continue;

                case "1":
                    createNewIdea(scanner);
                    System.out.print("Drücken Sie << Enter >> um fortzufahren: ");
                    scanner.nextLine();
                    continue;

                case "2":
                    createNewComment(scanner);
                    System.out.print("Drücken Sie << Enter >> um fortzufahren: ");
                    scanner.nextLine();
                    continue;

                case "3":
                    deleteIdea(scanner);
                    System.out.print("Drücken Sie << Enter >> um fortzufahren: ");
                    scanner.nextLine();
                    continue;

                default:
                    System.out.println("Brainstorming wird beendet.");
                    return;
            }
        }
    }
}
