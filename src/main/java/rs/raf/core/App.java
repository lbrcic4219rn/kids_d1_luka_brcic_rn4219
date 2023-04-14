package rs.raf.core;

import rs.raf.job.JobDispatcher;
import rs.raf.job.JobQueue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

public class App {
    public static final String KEYWORDS = "KEYWORDS";
    public static final String FILE_CORPUS_PREFIX = "FILE_CORPUS_PREFIX";
    public static final String DIR_CRAWLER_SLEEP_TIME = "DIR_CRAWLER_SLEEP_TIME";
    public static final String FILE_SCANNING_SIZE_LIMIT = "FILE_SCANNING_SIZE_LIMIT";
    public static final String HOP_COUNT = "HOP_COUNT";
    public static final String URL_REFRESH_TIME = "URL_REFRESH_TIME";
    public static Map<String, String> properties = new HashMap<>();
    public static Map<String, Long> visitedPages = new ConcurrentHashMap<>();

    public App() {

    }

    public void readConfig() {
        Scanner s = null;
        try {
            s = new Scanner(new File("./src/main/resources/app.properties"));

            while(s.hasNextLine()) {
                String line = s.nextLine().trim();

                if(line.startsWith("#") || line.equals(""))
                    continue;

                String[] keyValue = line.split("=");
                String key = keyValue[0].trim().toUpperCase();
                String value = keyValue[1].trim();
                properties.put(key, value);
            }
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        } finally {
            if(s != null) s.close();
        }
    }

    public void handleCommands () {
        CommandHandler commandHandler = new CommandHandler();
        commandHandler.start();

        Scanner s = new Scanner(System.in);
        while(true) {
            System.out.print("Enter a command: ");
            String line = s.nextLine().trim();
            String[] commandParam = line.split(" ");
            String command = commandParam[0];
            String param = "";
            if(commandParam.length == 2)
                param = commandParam[1];

            int paramCount = commandParam.length - 1;

            switch(command) {
                case "ad":
                    commandHandler.addDir(param, paramCount);
                    break;
                case "aw":
                    commandHandler.addWeb(param, paramCount);
                    break;
                case "get":
                    commandHandler.getResult(param, paramCount);
                    break;
                case "query":
                    commandHandler.queryResult(param, paramCount);
                    break;
                case "cws":
                    commandHandler.clearWebSummary(paramCount);
                    break;
                case "cfs":
                    commandHandler.clearFileSummary(paramCount);
                    break;
                case "stop":
                    commandHandler.stop();
                    s.close();
                    return;
                default:
                    System.out.println("Invalid command");

            }
        }
    }

    public static synchronized boolean isWebPageVisited(String url) {
        if (visitedPages.get(url) != null && System.currentTimeMillis() - visitedPages.get(url) < Long.parseLong(properties.get(URL_REFRESH_TIME))) {
            System.out.println("Already visited: " + url);
            return true;
        }
        visitedPages.put(url, System.currentTimeMillis());
        return false;
    }
}
