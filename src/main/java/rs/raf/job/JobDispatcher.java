package rs.raf.job;

import rs.raf.pools.FileThreadPool;
import rs.raf.pools.WebThreadPool;
import rs.raf.result.ResultRetrieverImpl;

import java.util.Map;
import java.util.concurrent.Future;

public class JobDispatcher extends Thread {

    private JobQueue jobQueue;
    private FileThreadPool fileThreadPool;
    private WebThreadPool webThreadPool;
    private ResultRetrieverImpl resultRetriever;
    private boolean working = true;

    public JobDispatcher(JobQueue jobQueue, ResultRetrieverImpl resultRetriever) {
        this.jobQueue = jobQueue;
        this.fileThreadPool = new FileThreadPool();
        this.webThreadPool = new WebThreadPool(10);
        this.resultRetriever = resultRetriever;
    }

    @Override
    public void run() {
        this.dispatchJobs();
    }

    public void terminate() {
        fileThreadPool.getPool().shutdownNow();
        webThreadPool.getPool().shutdownNow();
        System.out.println("stopping dispatcher...");
        working = false;
    }

    private void dispatchJobs() {
        while(working) {
            try {
                Job job = this.jobQueue.takeJob();
                switch (job.getType()) {
                    case FILE:
                        FileJob fileJob = (FileJob) job;
                        System.out.println("Starting file job: " + fileJob.getPath());
                        Future<Map<String, Integer>> fileResult = fileThreadPool.getPool().submit(fileJob);
                        resultRetriever.addCorpusResult(fileJob, fileResult);
                        break;
                    case WEB:
                        WebJob webJob = (WebJob) job;
                        System.out.println("Starting web job: " + webJob.getPath());
                        Future<Map<String, Integer>> webResult = webThreadPool.getPool().submit(webJob);
                        resultRetriever.addCorpusResult(webJob, webResult);
                        break;
                    case STOP:
                        terminate();
                        break;
                }

            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        System.out.println("Job dispatcher stopped");
    }
}
