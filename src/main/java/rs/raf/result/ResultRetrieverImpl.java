package rs.raf.result;

import rs.raf.job.FileJob;
import rs.raf.job.Job;
import rs.raf.job.ScanType;
import rs.raf.job.WebJob;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ResultRetrieverImpl implements ResultRetriever{
    private ExecutorService pool;
    private Map<String, Future<Map<String, Integer>>> fileResults = new ConcurrentHashMap<>();
    private Map<String, Future<Map<String, Integer>>> webResults = new ConcurrentHashMap<>();

    private Map<String, Map<String, Integer>> fileSummaryResults = new ConcurrentHashMap<>();
    private Map<String, Map<String, Integer>> webSummaryResults = new ConcurrentHashMap<>();
    private Map<String, Map<String, Integer>> domainWebResults = new ConcurrentHashMap<>();
    public ResultRetrieverImpl(int n) {
        this.pool = Executors.newFixedThreadPool(n);
    }

    public ExecutorService getPool() {
        return pool;
    }

    public Future<Map<String, Map<String, Integer>>> retrieveResult(String query, String command) {
        return getPool().submit(new ResultJob(query, this, command));
    }

    @Override
    public Map<String, Map<String, Integer>> singleResult(String query, String command) {
        String[] params = query.split("\\|");
        if(params.length != 2){
            System.out.println("Wrong query format: " + query);
            return null;
        }
        String type = params[0];
        String path = params[1];
        if(type.equals("file")) {
            if(fileResults.containsKey(path)){
                if(command.equals("query") && !fileResults.get(path).isDone()){
                    System.out.println(path + " is still being scanned");
                    return null;
                }
                try {
                    Map<String, Integer> map = fileResults.get(path).get();
                    Map<String, Map<String, Integer>> result = new ConcurrentHashMap<>();
                    result.put(path, map);
                    if(command.equals("query"))
                        System.out.println(result);
                    return result;
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            } else {
                System.out.println("File " + path + "is still being scanned");
                return null;
            }
        }
        if(type.equals("web")) {
            Map<String, Map<String, Integer>> result = new ConcurrentHashMap<>();

            if(domainWebResults.get(path) != null) {
                result.put(path, domainWebResults.get(path));
                if(command.equals(query))
                    System.out.println(result);
            }

            Map<String, Integer> domainSum = new ConcurrentHashMap<>();
            boolean isDomainPresent = false;
            for(String key : webResults.keySet()) {
                try {
                    String domain = new URL(key).getHost();
                    isDomainPresent = true;
                    if(command.equals("query") && !webResults.get(key).isDone()){
                        System.out.println("Webpage " + path + " is still being scanned");
                        return null;
                    }

                    domainSum = Stream.concat(domainSum.entrySet().stream(), webResults.get(key).get().entrySet().stream())
                            .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));

                } catch (MalformedURLException | ExecutionException | InterruptedException e) {
//                } catch (MalformedURLException e) {
                    System.err.println(key + " : " + e.getMessage());
                }
            }
            if (!isDomainPresent) {
                System.out.println("Domain is not present: " + path);
                return null;
            }

            result.put(path, domainSum);

            if (command.equals("query")) {
                System.out.println(result);
            }
            return result;
        }
        return null;
    }

    @Override
    public Map<String, Map<String, Integer>> summaryResult(String query, String command) {
        String[] params = query.split("\\|");
        if(params.length != 2){
            System.out.println("Wrong query format: " + query);
            return null;
        }
        String type = params[0];
        if(type.equals("file")) {
            if(!fileSummaryResults.isEmpty()){
                if(command.equals("query")){
                    fileSummaryResults.forEach((k, v) -> System.out.println(k + " : " + v));
                    return fileSummaryResults;
                }
            } else {
                if(command.equals("query")) {
                    for(String key : fileResults.keySet()) {
                        if(!fileResults.get(key).isDone()) {
                            System.out.println("File summary not ready");
                            return null;
                        }
                    }
                }
                for(String key : fileResults.keySet()) {
                    try {
                        this.fileSummaryResults.put(key, fileResults.get(key).get());
                    } catch (InterruptedException | ExecutionException e) {
                        throw new RuntimeException(e);
                    }
                }
                if(command.equals("query"))
                    fileSummaryResults.forEach((k, v) -> System.out.println(k + " : " + v));
            }
            return fileSummaryResults;
        }
        if(type.equals("web")) {
            if(!webSummaryResults.isEmpty()){
                if(command.equals("query")){
                    webSummaryResults.forEach((k, v) -> System.out.println(k + " : " + v));
                }
                return webSummaryResults;
            }

            for (String key : webResults.keySet()) {
                if (command.equals("query")) {
                    if (!webResults.get(key).isDone()) {
                        System.out.println("Webpage " + key + " is still being scanned");
                        return null;
                    }
                }
            }

            for (String key : webResults.keySet()) {
                try {
                    URL url;
                    url = new URL(key);
                    String domain = url.getHost();
                    if (webSummaryResults.get(domain) == null) {
                        webSummaryResults.put(domain, webResults.get(key).get());
                    }
                    else {
                        Map<String, Integer> domainMap = webSummaryResults.get(domain);
                        domainMap = Stream.concat(domainMap.entrySet().stream(), webResults.get(key).get().entrySet().stream())
                                .collect(Collectors.groupingBy(Map.Entry::getKey, Collectors.summingInt(Map.Entry::getValue)));
                        webSummaryResults.put(domain, domainMap);
                    }
                }
                catch (MalformedURLException | InterruptedException | ExecutionException e) {
                    System.err.println(key + " {" + e.getMessage() +"}");
                }
            }

            if (command.equals("query")) {
                webSummaryResults.forEach((k, v) -> System.out.println(k + " : " + v));
            }

            return webSummaryResults;
        }
        return null;
    }

    @Override
    public void addCorpusResult(Job submittedJob, Future<Map<String, Integer>> jobFutureResult) {
        switch (submittedJob.getType()) {
            case FILE:
                FileJob fileJob = (FileJob) submittedJob;
                this.fileResults.put(fileJob.getPath(), jobFutureResult);
                break;
            case WEB:
                WebJob webJob = (WebJob) submittedJob;
                this.webResults.put(webJob.getPath(), jobFutureResult);
                break;
        }
    }

    @Override
    public void clearSummary(ScanType summaryType) {
        if(summaryType.equals(ScanType.FILE))
            this.fileSummaryResults.clear();
        if (summaryType.equals(ScanType.WEB))
            this.webSummaryResults.clear();
    }
}
