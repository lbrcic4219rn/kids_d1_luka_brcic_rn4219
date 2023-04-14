package rs.raf.core;

import rs.raf.crawler.DirectoryCrawler;
import rs.raf.job.JobDispatcher;
import rs.raf.job.JobQueue;
import rs.raf.job.StopJob;
import rs.raf.job.WebJob;
import rs.raf.result.ResultRetrieverImpl;

import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class CommandHandler {
    private final JobQueue jobQueue;
    private final DirectoryCrawler directoryCrawler;
    private final ResultRetrieverImpl resultRetriever;
    private JobDispatcher jobDispatcher;

    public CommandHandler() {
        this.jobQueue = new JobQueue();;
        this.resultRetriever = new ResultRetrieverImpl(10);
        this.directoryCrawler = new DirectoryCrawler(this.jobQueue);
    }


    public void addDir(String param, int paramCount) {
        if(isParamValid(paramCount, 1))
            this.directoryCrawler.addPath(param);
    }

    public void addWeb(String param, int paramCount) {
        if(isParamValid(paramCount, 1)){
            try {
                jobQueue.addJob(new WebJob(param, Integer.parseInt(App.properties.get(App.HOP_COUNT)), jobQueue));
            } catch (InterruptedException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }
        }
    }

    public void getResult(String param, int paramCount) {
        if(isParamValid(paramCount, 1)) {
            Future<Map<String, Map<String, Integer>>> future = this.resultRetriever.retrieveResult(param, "get");
            System.out.println("Getting results...");
            try {
                Map<String, Map<String, Integer>> result = future.get();
                if (result != null) {
                    result.forEach((k, v) -> System.out.println((k + " : " + v)));
                }
            } catch (InterruptedException | ExecutionException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public void queryResult(String param, int paramCount) {
        if(isParamValid(paramCount, 1)) {
            this.resultRetriever.retrieveResult(param, "query");
        }
    }

    public void clearWebSummary(int paramCount) {
        if(isParamValid(paramCount, 0)) {
            this.resultRetriever.retrieveResult("", "cws");
        }
    }

    public void clearFileSummary(int paramCount) {
        if(isParamValid(paramCount, 0)) {
            this.resultRetriever.retrieveResult("", "cfs");
        }
    }

    public void stop() {
        System.out.println("stopping...");
        this.resultRetriever.getPool().shutdownNow();
        this.directoryCrawler.stop();
        try {
            jobQueue.addJob(new StopJob());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isParamValid(int paramCount, int expectedParamCount) {
        if(paramCount != expectedParamCount){
            System.out.println("Expected: " + expectedParamCount + " parameters, but got: " + paramCount);
            return false;
        }
        return true;
    }

    public void start() {
        this.directoryCrawler.start();
        this.jobDispatcher = new JobDispatcher(this.jobQueue, this.resultRetriever);
        jobDispatcher.start();
    }
}
